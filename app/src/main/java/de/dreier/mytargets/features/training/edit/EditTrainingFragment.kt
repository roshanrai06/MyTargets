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
package de.dreier.mytargets.features.training.edit

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.databinding.DataBindingUtil
import de.dreier.mytargets.R
import de.dreier.mytargets.app.ApplicationInstance
import de.dreier.mytargets.base.fragments.EditFragmentBase
import de.dreier.mytargets.base.fragments.EditableListFragmentBase.Companion.ITEM_ID
import de.dreier.mytargets.base.navigation.NavigationController.Companion.ITEM
import de.dreier.mytargets.databinding.FragmentEditTrainingBinding
import de.dreier.mytargets.features.settings.SettingsManager
import de.dreier.mytargets.features.training.ETrainingType
import de.dreier.mytargets.features.training.ETrainingType.FREE_TRAINING
import de.dreier.mytargets.features.training.ETrainingType.TRAINING_WITH_STANDARD_ROUND
import de.dreier.mytargets.features.training.target.TargetListFragment
import de.dreier.mytargets.shared.models.EBowType
import de.dreier.mytargets.shared.models.Target
import de.dreier.mytargets.shared.models.db.Bow
import de.dreier.mytargets.shared.models.db.Round
import de.dreier.mytargets.shared.models.db.Training
import de.dreier.mytargets.shared.targets.models.WA3Ring3Spot
import de.dreier.mytargets.utils.ToolbarUtils
import de.dreier.mytargets.utils.Utils
import de.dreier.mytargets.utils.getLongOrNull
import de.dreier.mytargets.utils.parcelableExtra
import de.dreier.mytargets.views.selector.ArrowSelector
import de.dreier.mytargets.views.selector.BowSelector
import com.google.android.material.slider.Slider
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

class EditTrainingFragment : EditFragmentBase(), DatePickerDialog.OnDateSetListener {

    private var trainingId: Long? = null
    private var trainingType = FREE_TRAINING
    private var date: LocalDate = LocalDate.now()
    private lateinit var binding: FragmentEditTrainingBinding
    private var roundTarget: Target? = null

    private val database = ApplicationInstance.db
    private val trainingDAO = database.trainingDAO()

    private val training: Training
        get() {
            val training = if (trainingId == null) {
                Training()
            } else {
                trainingDAO.loadTraining(trainingId!!)
            }
            training.title = binding.training.text.toString()
            training.date = date
            training.environment = binding.environment.selectedItem!!
            training.bowId = binding.bow.selectedItem?.id
            training.arrowId = binding.arrow.selectedItem?.id
            training.arrowNumbering = binding.numberArrows.isChecked

            SettingsManager.bow = training.bowId
            SettingsManager.arrow = training.arrowId
            SettingsManager.arrowNumbersEnabled = training.arrowNumbering
            SettingsManager.indoor = training.environment.indoor
            return training
        }

    private val round: Round
        get() {
            val round = Round()
            round.target = binding.target.selectedItem!!
            round.shotsPerEnd = binding.arrows.value.toInt()
            round.maxEndCount = null
            round.distance = binding.distance.selectedItem!!

            SettingsManager.target = binding.target.selectedItem!!
            SettingsManager.distance = round.distance
            SettingsManager.shotsPerEnd = round.shotsPerEnd
            return round
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_edit_training, container, false)

        trainingId = arguments.getLongOrNull(ITEM_ID)
        trainingType = if (activity?.intent?.action == CREATE_TRAINING_WITH_STANDARD_ROUND_ACTION) {
            TRAINING_WITH_STANDARD_ROUND
        } else {
            FREE_TRAINING
        }

        ToolbarUtils.setSupportActionBar(this, binding.toolbar)
        ToolbarUtils.showUpAsX(this)
        setHasOptionsMenu(true)

        binding.arrows.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateArrowsLabel()
            }
        }
        binding.target.setOnClickListener { selectedItem, index ->
            navigationController.navigateToTarget(selectedItem!!, index)
        }
        binding.distance.setOnClickListener { selectedItem, index ->
            navigationController.navigateToDistance(selectedItem!!, index)
        }
        binding.standardRound.setOnClickListener { selectedItem, _ ->
            navigationController.navigateToStandardRoundList(selectedItem!!)
        }
        binding.standardRound.setOnUpdateListener { item ->
            roundTarget = item!!.roundTemplates[0].targetTemplate
        }
        binding.changeTargetFace.setOnClickListener {
            navigationController.navigateToTarget(
                roundTarget!!,
                requestCode = SR_TARGET_REQUEST_CODE,
                fixedType = TargetListFragment.EFixedType.GROUP
            )
        }
        binding.arrow.setOnAddClickListener {
            navigationController.navigateToCreateArrow()
                .forResult(ArrowSelector.ARROW_ADD_REQUEST_CODE)
                .start()
        }
        binding.arrow.setOnClickListener { selectedItem, _ ->
            navigationController.navigateToArrowList(selectedItem!!)
        }
        binding.bow.setOnAddClickListener {
            navigationController.navigateToCreateBow(EBowType.RECURVE_BOW)
                .forResult(BowSelector.BOW_ADD_REQUEST_CODE)
                .start()
        }
        binding.bow.setOnClickListener { selectedItem, _ ->
            navigationController.navigateToBowList(selectedItem!!)
        }
        binding.bow.setOnUpdateListener { this.setScoringStyleForCompoundBow(it) }
        binding.environment.setOnClickListener { selectedItem, _ ->
            navigationController.navigateToEnvironment(selectedItem!!)
        }
        binding.trainingDate.setOnClickListener { onDateClick() }

        if (trainingId == null) {
            ToolbarUtils.setTitle(this, R.string.new_training)
            binding.training.setText(
                getString(
                    if (trainingType == ETrainingType.COMPETITION)
                        R.string.competition
                    else
                        R.string.training
                )
            )
            setTrainingDate()
            loadRoundDefaultValues()
            binding.bow.setItemId(SettingsManager.bow)
            binding.arrow.setItemId(SettingsManager.arrow)
            binding.standardRound.setItemId(SettingsManager.standardRound)
            binding.numberArrows.isChecked = SettingsManager.arrowNumbersEnabled
            if (savedInstanceState == null) {
                binding.environment.queryWeather(this, REQUEST_LOCATION_PERMISSION)
            }
            binding.changeTargetFace.visibility = if (trainingType == TRAINING_WITH_STANDARD_ROUND)
                VISIBLE
            else
                GONE
        } else {
            ToolbarUtils.setTitle(this, R.string.edit_training)
            val train = trainingDAO.loadTraining(trainingId!!)
            binding.training.setText(train.title)
            date = train.date
            binding.bow.setItemId(train.bowId)
            binding.arrow.setItemId(train.arrowId)
            binding.environment.setItem(train.environment)
            setTrainingDate()
            binding.notEditable.visibility = GONE
            binding.changeTargetFace.visibility =
                    if (train.standardRoundId != null) VISIBLE else GONE
        }
        applyTrainingType()
        updateArrowsLabel()

        return binding.root
    }

    private fun updateArrowsLabel() {
        val value = binding.arrows.value.toInt()
        binding.arrowsLabel.text = resources
            .getQuantityString(R.plurals.arrow, value, value)
    }

    private fun setScoringStyleForCompoundBow(bow: Bow?) {
        val target = binding.target.selectedItem
        if (bow != null && target != null && target.id <= WA3Ring3Spot.ID) {
            if (bow.type === EBowType.COMPOUND_BOW && target.scoringStyleIndex == 0) {
                target.scoringStyleIndex = 2
                binding.target.setItem(target)
            } else if (bow.type !== EBowType.COMPOUND_BOW && target.scoringStyleIndex == 2) {
                target.scoringStyleIndex = 0
                binding.target.setItem(target)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.setupFabTransform(requireActivity(), binding.root)
    }

    private fun applyTrainingType() {
        if (trainingType == FREE_TRAINING) {
            binding.practiceLayout.visibility = VISIBLE
            binding.standardRound.visibility = GONE
        } else {
            binding.practiceLayout.visibility = GONE
            binding.standardRound.visibility = VISIBLE
        }
    }

    private fun onDateClick() {
        val datePickerDialog = DatePickerFragment.newInstance(date)
        datePickerDialog.setTargetFragment(this, REQ_SELECTED_DATE)
        datePickerDialog.show(requireActivity().supportFragmentManager, "date_picker")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            binding.environment.onPermissionResult(requireActivity(), grantResults)
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        date = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
        setTrainingDate()
    }

    private fun setTrainingDate() {
        binding.trainingDate.text =
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    override fun onSave() {
        navigationController.finish()

        val training = training

        if (trainingId == null) {
            val rounds: MutableList<Round>
            if (trainingType == FREE_TRAINING) {
                training.standardRoundId = null
                rounds = mutableListOf(round)
            } else {
                val standardRound = binding.standardRound.selectedItem!!
                if (standardRound.standardRound.id == 0L) {
                    throw IllegalStateException("I assumed the standard round would have already been saved")
                    //StandardRoundDAO.insertStandardRound(standardRound.standardRound, standardRound.roundTemplates)
                }
                SettingsManager.standardRound = standardRound.standardRound.id
                training.standardRoundId = standardRound.standardRound.id
                rounds = standardRound.createRoundsFromTemplate()
                for (round in rounds) {
                    round.target = roundTarget!!
                }
            }
            trainingDAO.insertTraining(training, rounds)

            val round = rounds[0]

            navigationController.navigateToTraining(training)
                .noAnimation()
                .start()
            navigationController.navigateToRound(round)
                .noAnimation()
                .start()
            navigationController.navigateToCreateEnd(round)
        } else {
            // Edit training
            trainingDAO.updateTraining(training)
            requireActivity().overridePendingTransition(R.anim.left_in, R.anim.right_out)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.target.onActivityResult(requestCode, resultCode, data)
        binding.distance.onActivityResult(requestCode, resultCode, data)
        binding.standardRound.onActivityResult(requestCode, resultCode, data)
        binding.arrow.onActivityResult(requestCode, resultCode, data)
        binding.bow.onActivityResult(requestCode, resultCode, data)
        binding.environment.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SR_TARGET_REQUEST_CODE && data != null) {
            val target = data.parcelableExtra<Target>(data, ITEM)
            val item = binding.standardRound.selectedItem
            item?.roundTemplates?.forEach {
                if (target != null) {
                    it.targetTemplate = target
                }
            }
            binding.standardRound.setItem(item)
        }
    }

    private fun loadRoundDefaultValues() {
        binding.distance.setItem(SettingsManager.distance)
        binding.arrows.value = SettingsManager.shotsPerEnd.toFloat()
        binding.target.setItem(SettingsManager.target)
    }

    companion object {
        const val CREATE_FREE_TRAINING_ACTION = "free_training"
        const val CREATE_TRAINING_WITH_STANDARD_ROUND_ACTION = "with_standard_round"

        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQ_SELECTED_DATE = 2
        private const val SR_TARGET_REQUEST_CODE = 11
    }
}
