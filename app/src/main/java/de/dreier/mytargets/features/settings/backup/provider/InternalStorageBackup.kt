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

package de.dreier.mytargets.features.settings.backup.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import de.dreier.mytargets.R
import de.dreier.mytargets.app.ApplicationInstance
import de.dreier.mytargets.features.settings.backup.BackupEntry
import de.dreier.mytargets.features.settings.backup.BackupException
import de.dreier.mytargets.shared.SharedApplicationInstance.Companion.context
import de.dreier.mytargets.shared.SharedApplicationInstance.Companion.getStr
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.channels.FileChannel

object InternalStorageBackup {
    private const val FOLDER_NAME = "MyTargets"

    @Throws(IOException::class)
    private fun createDirectory(directory: File) {

        directory.mkdir()
        if (!directory.exists() || !directory.isDirectory) {
            throw IOException(getStr(R.string.dir_not_created))
        }
    }

    class AsyncRestore : IAsyncBackupRestore {

        private var context: WeakReference<Context>? = null

        override fun connect(context: Context, listener: IAsyncBackupRestore.ConnectionListener) {
            this.context = WeakReference(context)
            listener.onConnected()
        }

        override fun getBackups(listener: IAsyncBackupRestore.OnLoadFinishedListener) {
            val backupDir = File(context?.get()?.let { getStorageDirectory(it) }, FOLDER_NAME)
            if (backupDir.isDirectory) {
                val backups = backupDir.listFiles()
                    ?.filter { isBackup(it) }
                    ?.map {
                        BackupEntry(
                            it.absolutePath,
                            it.lastModified(),
                            it.length()
                        )
                    }
                    ?.sortedByDescending { it.lastModifiedAt }
                listener.onLoadFinished(backups ?: emptyList())
            }
        }

        private fun isBackup(file: File): Boolean {
            return file.isFile && file.name.contains("backup_") && file.name
                .endsWith(".zip")
        }

        override fun restoreBackup(
            backup: BackupEntry,
            listener: IAsyncBackupRestore.BackupStatusListener
        ) {
            val file = File(backup.fileId)
            try {
                BackupUtils.importZip(context!!.get()!!, FileInputStream(file))
                listener.onFinished()
            } catch (e: IOException) {
                listener.onError(e.localizedMessage)
                e.printStackTrace()
            }

        }

        override fun deleteBackup(
            backup: BackupEntry,
            listener: IAsyncBackupRestore.BackupStatusListener
        ) {
            if (File(backup.fileId).delete()) {
                listener.onFinished()
            } else {
                listener.onError("Backup could not be deleted!")
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            return false
        }
    }

    class Backup : IBlockingBackup {

        @Throws(BackupException::class)
        override fun performBackup(context: Context) {
            try {
                val backupDir = File(
                    getStorageDirectory(context),
                    FOLDER_NAME
                )
                createDirectory(backupDir)
                val zipFile = File(backupDir, BackupUtils.backupName)
                BackupUtils.zip(context, ApplicationInstance.db, FileOutputStream(zipFile))
            } catch (e: IOException) {
                throw BackupException(e.localizedMessage, e)
            }

        }
    }

    private fun getStorageDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.getExternalFilesDirs(context, null)[0] // Scoped Storage
        } else {
            Environment.getExternalStorageDirectory() // Legacy Storage
        }
    }

    private fun copyFileToScopedStorage(contentResolver: ContentResolver, zipFile: File): Uri? {
        val displayName = BackupUtils.backupName // Set the desired display name for the file

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        var outputStream: FileOutputStream? = null
        var inputStream: FileInputStream? = null
        var fileChannelOut: FileChannel? = null
        var fileChannelIn: FileChannel? = null

        return try {
            val contentUri: Uri? = contentResolver.insert(collection, contentValues)
            outputStream =
                contentUri?.let { contentResolver.openOutputStream(it) as FileOutputStream? }
            inputStream = FileInputStream(zipFile)
            BackupUtils.zip(context, ApplicationInstance.db, FileOutputStream(zipFile))
            fileChannelOut = outputStream?.channel
            fileChannelIn = inputStream.channel
            fileChannelIn.transferTo(0, fileChannelIn.size(), fileChannelOut)
            contentUri
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            fileChannelIn?.close()
            inputStream?.close()
            fileChannelOut?.close()
            outputStream?.close()
        }
    }

}
