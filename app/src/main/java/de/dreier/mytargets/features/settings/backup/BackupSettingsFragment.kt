/*
 * Copyright (C) 2018 Florian Dreier
 *
 * This file is part of MyTargets.
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package de.dreier.mytargets.features.settings.backup

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
import android.content.ContentResolver.SYNC_OBSERVER_TYPE_PENDING
import android.content.Context
import android.content.Intent
import android.content.SyncStatusObserver
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.afollestad.materialdialogs.MaterialDialog
import de.dreier.mytargets.R
import de.dreier.mytargets.app.ApplicationInstance
import de.dreier.mytargets.databinding.FragmentBackupBinding
import de.dreier.mytargets.features.settings.SettingsFragmentBase
import de.dreier.mytargets.features.settings.SettingsManager
import de.dreier.mytargets.features.settings.backup.provider.BackupUtils
import de.dreier.mytargets.features.settings.backup.provider.EBackupLocation
import de.dreier.mytargets.features.settings.backup.provider.IAsyncBackupRestore
import de.dreier.mytargets.features.settings.backup.synchronization.GenericAccountService
import de.dreier.mytargets.features.settings.backup.synchronization.SyncUtils
import de.dreier.mytargets.utils.ToolbarUtils
import de.dreier.mytargets.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import timber.log.Timber
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class BackupSettingsFragment : SettingsFragmentBase(), IAsyncBackupRestore.OnLoadFinishedListener {

    private var backup: IAsyncBackupRestore? = null
    private var adapter: BackupAdapter? = null
    private lateinit var binding: FragmentBackupBinding
    private var updateLabelTimer: Timer? = null
    private var isLeaving = false

    /**
     * Handle to a SyncObserver. The ProgressBar element is visible until the SyncObserver reports
     * that the sync is complete.
     *
     * This allows us to delete our SyncObserver once the application is no longer in the
     * foreground.
     */
    private var syncObserverHandle: Any? = null
    private var isRefreshing = false

    /**
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If status changes, it sets the state of the Progress
     * bar. If a sync is active or pending, the progress is shown.
     */
    private val syncStatusObserver = SyncStatusObserver {
        activity?.runOnUiThread {
            val account = GenericAccountService.account
            val syncActive = ContentResolver.isSyncActive(account, SyncUtils.CONTENT_AUTHORITY)
            val syncPending = ContentResolver.isSyncPending(account, SyncUtils.CONTENT_AUTHORITY)
            val wasRefreshing = isRefreshing
            isRefreshing = syncActive || syncPending
            binding.backupNowButton.isEnabled = !isRefreshing
            binding.backupProgressBar.isIndeterminate = true
            binding.backupProgressBar.visibility = if (isRefreshing) VISIBLE else GONE
            if (wasRefreshing && !isRefreshing && backup != null) {
                backup?.getBackups(this)
            }
        }
    }

    /**
     * Create SyncAccount at launch, if needed.
     *
     * This will create a new account with the system for our application and register our
     * [de.dreier.mytargets.features.settings.backup.synchronization.SyncService] with it.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Create account, if needed
        SyncUtils.createSyncAccount(context)
    }

    public override fun onCreatePreferences() {
        /* Overridden to no do anything. Normally this would try to inflate the preferences,
        * but in this case we want to show our own UI. */
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup, container, false)
        ToolbarUtils.showHomeAsUp(this)

        binding.backupNowButton.setOnClickListener { SyncUtils.triggerBackup() }

        binding.automaticBackupSwitch.setOnClickListener { onAutomaticBackupChanged() }

        binding.backupIntervalPreference.root.setOnClickListener { onBackupIntervalClicked() }
        binding.backupIntervalPreference.image
            .setImageResource(R.drawable.ic_query_builder_grey600_24dp)
        binding.backupIntervalPreference.name.setText(R.string.backup_interval)
        updateInterval()

        binding.backupLocation.root.setOnClickListener { onBackupLocationClicked() }

        binding.recentBackupsList.isNestedScrollingEnabled = false
        binding.recentBackupsList
            .addItemDecoration(DividerItemDecoration(context!!, VERTICAL))

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.settings_backup, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_import) {
            checkReadStoragePermissionAndShowFilePicker()
            return true
        } else if (item.itemId == R.id.action_fix_db) {
            DatabaseFixer.fix(ApplicationInstance.db)
            return true
        } else if (item.itemId == R.id.action_remove_photos) {
            deleteAllPhotos()

        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteAllPhotos() {
        lifecycleScope.launch(Dispatchers.IO) {  // Launch coroutine on IO dispatcher (background thread)
            ApplicationInstance.db.imageDAO().removeAllPhotos(requireContext())

            // Back on the main thread for UI updates (Toast)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "All photos removed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLeaving) {
            checkWriteStoragePermissionAndApplyBackupLocation(SettingsManager.backupLocation)
            updateAutomaticBackupSwitch()
        }

        syncStatusObserver.onStatusChanged(0)
        syncObserverHandle = ContentResolver.addStatusChangeListener(
            SYNC_OBSERVER_TYPE_PENDING or SYNC_OBSERVER_TYPE_ACTIVE, syncStatusObserver
        )
    }

    override fun onPause() {
        updateLabelTimer?.cancel()
        if (syncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncObserverHandle)
            syncObserverHandle = null
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        backup?.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_FROM_URI && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            data.data?.let { importFromUri(it) }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onAutomaticBackupChanged() {
        val autoBackupEnabled = binding.automaticBackupSwitch.isChecked
        binding.backupIntervalLayout.visibility = if (autoBackupEnabled) VISIBLE else GONE
        SyncUtils.isSyncAutomaticallyEnabled = autoBackupEnabled
    }

    private fun updateAutomaticBackupSwitch() {
        val autoBackupEnabled = SyncUtils.isSyncAutomaticallyEnabled
        binding.automaticBackupSwitch.isChecked = autoBackupEnabled
    }

    private fun onBackupIntervalClicked() {
        val backupIntervals = listOf(*EBackupInterval.values())
        MaterialDialog.Builder(context!!)
            .title(R.string.backup_interval)
            .items(backupIntervals)
            .itemsCallbackSingleChoice(
                backupIntervals.indexOf(SettingsManager.backupInterval)
            ) { _, _, index, _ ->
                SettingsManager.backupInterval = EBackupInterval.values()[index]
                updateInterval()
                true
            }
            .show()
    }

    private fun updateInterval() {
        val autoBackupEnabled = SyncUtils.isSyncAutomaticallyEnabled
        binding.backupIntervalLayout.visibility = if (autoBackupEnabled) VISIBLE else GONE
        binding.backupIntervalPreference.summary.text = SettingsManager.backupInterval.toString()
    }

    private fun onBackupLocationClicked() {
        val item = SettingsManager.backupLocation
        MaterialDialog.Builder(context!!)
            .title(R.string.backup_location)
            .items(EBackupLocation.list)
            .itemsCallbackSingleChoice(
                EBackupLocation.list.indexOf(item)
            ) { _, _, index, _ ->
                val location = EBackupLocation.list[index]
                internalApplyBackupLocationWithPermissionCheck(location)
                true
            }
            .show()
    }

    private fun updateBackupLocation() {
        val backupLocation = SettingsManager.backupLocation
        binding.backupLocation.image.setImageResource(backupLocation.drawableRes)
        binding.backupLocation.name.setText(R.string.backup_location)
        binding.backupLocation.summary.text = backupLocation.toString()
    }

    private fun internalApplyBackupLocationWithPermissionCheck(item: EBackupLocation) {
        if (item.needsStoragePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retrieveBackup(item)
            } else {
                checkWriteStoragePermissionAndApplyBackupLocation(item)
            }

        } else {
            applyBackupLocation(item)
        }
    }

    internal fun applyBackupLocation(item: EBackupLocation) {
        retrieveBackup(item)
    }

    private fun retrieveBackup(item: EBackupLocation) {
        SettingsManager.backupLocation = item
        backup = item.createAsyncRestore()
        binding.recentBackupsProgress.visibility = VISIBLE
        binding.recentBackupsList.visibility = GONE
        adapter = BackupAdapter(context!!, object : OnItemClickListener<BackupEntry> {
            override fun onItemClicked(item: BackupEntry) {
                showBackupDetails(item)
            }
        }, object : OnItemClickListener<BackupEntry> {
            override fun onItemClicked(item: BackupEntry) {
                deleteBackup(item)
            }
        })
        binding.recentBackupsList.adapter = adapter
        backup?.connect(
            context!!,
            object : IAsyncBackupRestore.ConnectionListener {
                override fun onLoginCancelled() {
                    internalApplyBackupLocationWithPermissionCheck(EBackupLocation.INTERNAL_STORAGE)
                }

                override fun onStartIntent(intent: Intent, code: Int) {
                    startActivityForResult(intent, code)
                }

                override fun onConnected() {
                    updateBackupLocation()
                    backup?.getBackups(this@BackupSettingsFragment)
                }

                override fun onConnectionSuspended() {
                    showError(
                        R.string.loading_backups_failed,
                        getString(R.string.connection_failed)
                    )
                }
            })
    }


    override fun setActivityTitle() {
        activity!!.setTitle(R.string.backup_action)
    }

    private fun onBackupsLoaded(list: List<BackupEntry>) {
        binding.recentBackupsProgress.visibility = GONE
        binding.recentBackupsList.visibility = VISIBLE
        adapter?.setList(list.toMutableList())
        binding.lastBackupLabel.visibility = if (list.isNotEmpty()) VISIBLE else GONE
        updateLabelTimer?.cancel()
        if (list.isNotEmpty()) {
            val time = list[0].lastModifiedAt
            updateLabelTimer = Timer()
            val timerTask = object : TimerTask() {
                override fun run() {
                    activity!!.runOnUiThread {
                        binding.lastBackupLabel.text = getString(
                            R.string.last_backup, DateUtils
                                .getRelativeTimeSpanString(time)
                        )
                    }
                }
            }
            updateLabelTimer!!.schedule(timerTask, 0, 10000)
        }
    }

    private fun showError(@StringRes title: Int, message: String) {
        MaterialDialog.Builder(context!!)
            .title(title)
            .content(message)
            .positiveText(android.R.string.ok)
            .show()
    }

    private fun showRestoreProgressDialog(): MaterialDialog {
        return MaterialDialog.Builder(context!!)
            .content(R.string.restoring)
            .progress(true, 0)
            .show()
    }

    private fun showBackupDetails(item: BackupEntry) {
        val html = String.format(
            Locale.US,
            "%s<br><br><b>%s</b><br>%s<br>%s",
            getString(R.string.restore_desc),
            getString(R.string.backup_details),
            SimpleDateFormat.getDateTimeInstance()
                .format(item.lastModifiedAt),
            item.humanReadableSize
        )
        MaterialDialog.Builder(context!!)
            .title(R.string.dialog_restore_title)
            .content(Utils.fromHtml(html))
            .positiveText(R.string.restore)
            .negativeText(android.R.string.cancel)
            .positiveColor(-0x1ac6cb)
            .negativeColor(-0x78000000)
            .onPositive { _, _ -> restoreBackup(item) }
            .show()
    }

    private fun restoreBackup(item: BackupEntry) {
        val progress = showRestoreProgressDialog()
        backup?.restoreBackup(item,
            object : IAsyncBackupRestore.BackupStatusListener {
                override fun onFinished() {
                    progress.dismiss()
                    Utils.doRestart(context!!)
                }

                override fun onError(message: String) {
                    progress.dismiss()
                    showError(R.string.restore_failed, message)
                }
            })
    }

    private fun deleteBackup(backupEntry: BackupEntry) {
        backup?.deleteBackup(backupEntry, object : IAsyncBackupRestore.BackupStatusListener {
            override fun onFinished() {
                adapter!!.remove(backupEntry)
                backup?.getBackups(this@BackupSettingsFragment)
            }

            override fun onError(message: String) {
                showError(R.string.delete_failed, message)
            }
        })
    }

    private fun checkReadStoragePermissionAndShowFilePicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED) {
            showFilePicker()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_STORAGE_PERMISSION
            )
        }
    }

    private fun checkWriteStoragePermissionAndApplyBackupLocation(location: EBackupLocation) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED) {
            internalApplyBackupLocationWithPermissionCheck(location)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE_PERMISSION
            )
        }
    }

    internal fun showFilePicker() {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT)
        getContentIntent.type = "application/zip"
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE)
        val intent = Intent.createChooser(getContentIntent, getString(R.string.select_a_file))
        startActivityForResult(intent, IMPORT_FROM_URI)
    }

    private fun handleWritePermissionNeverAskAgain() {
        isLeaving = true
        MaterialDialog.Builder(context!!)
            .title(R.string.permission_required)
            .content(R.string.backup_permission_explanation)
            .positiveText(android.R.string.ok).negativeText(android.R.string.cancel)
            .onPositive { _, _ -> leaveBackupSettings() }
            .onNegative { _, _ -> onBackupLocationClicked() }
            .show()
    }

    private fun handleWritePermissionDenied() {
        leaveBackupSettings()
    }

    private fun leaveBackupSettings() {
        isLeaving = true
        val h = Handler()
        h.post { activity!!.supportFragmentManager.popBackStack() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_READ_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showFilePicker()
                }
            }
            REQUEST_WRITE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    internalApplyBackupLocationWithPermissionCheck(SettingsManager.backupLocation)
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(), 
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )) {
                        handleWritePermissionNeverAskAgain()
                    } else {
                        handleWritePermissionDenied()
                    }
                }
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        val progress = showRestoreProgressDialog()
        object : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void): String? {
                return try {
                    Timber.i("Importing backup from $uri")
                    val st = context!!.contentResolver.openInputStream(uri)
                    BackupUtils.importZip(context!!, st!!)
                    null
                } catch (ioe: FileNotFoundException) {
                    ioe.printStackTrace()
                    getString(R.string.file_not_found)
                } catch (e: Exception) {
                    e.printStackTrace()
                    getString(R.string.failed_reading_file)
                }
            }

            override fun onPostExecute(errorMessage: String?) {
                progress.dismiss()
                if (errorMessage == null) {
                    Utils.doRestart(context!!)
                } else {
                    showError(R.string.import_failed, errorMessage)
                }
            }
        }.execute()
    }

    override fun onLoadFinished(backupEntries: List<BackupEntry>) {
        onBackupsLoaded(backupEntries)
    }

    override fun onError(message: String) {
        showError(R.string.loading_backups_failed, message)
    }

    companion object {
        private const val IMPORT_FROM_URI = 1234
        private const val REQUEST_READ_STORAGE_PERMISSION = 1235
        private const val REQUEST_WRITE_STORAGE_PERMISSION = 1236
    }
}
