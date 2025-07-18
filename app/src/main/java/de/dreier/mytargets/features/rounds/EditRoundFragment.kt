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
package de.dreier.mytargets.features.rounds

import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.dreier.mytargets.R
import de.dreier.mytargets.app.ApplicationInstance
import de.dreier.mytargets.base.fragments.EditFragmentBase
import de.dreier.mytargets.base.fragments.EditableListFragmentBase.Companion.ITEM_ID
import de.dreier.mytargets.databinding.FragmentEditRoundBinding
import de.dreier.mytargets.features.settings.SettingsManager
import de.dreier.mytargets.features.training.target.TargetListFragment
import de.dreier.mytargets.shared.models.db.Round
import de.dreier.mytargets.utils.ToolbarUtils
import de.dreier.mytargets.utils.Utils
import de.dreier.mytargets.views.selector.DistanceSelector
import de.dreier.mytargets.views.selector.TargetSelector
import com.google.android.material.slider.Slider

class EditRoundFragment : EditFragmentBase() {
    private var trainingId: Long = 0
    private var roundId: Long? = null
    private lateinit var binding: FragmentEditRoundBinding

    private val trainingDAO = ApplicationInstance.db.trainingDAO()
    private val roundDAO = ApplicationInstance.db.roundDAO()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_edit_round, container, false)

        trainingId = arguments!!.getLong(ITEM_ID)
        if (arguments!!.containsKey(ROUND_ID)) {
            roundId = arguments!!.getLong(ROUND_ID)
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
            val fixedType =
                if (roundId == null) TargetListFragment.EFixedType.NONE else TargetListFragment.EFixedType.TARGET
            navigationController.navigateToTarget(
                selectedItem!!,
                index,
                TargetSelector.TARGET_REQUEST_CODE,
                fixedType
            )
        }
        binding.distance.setOnClickListener { selectedItem, index ->
            navigationController.navigateToDistance(
                selectedItem!!,
                index,
                DistanceSelector.DISTANCE_REQUEST_CODE
            )
        }

        if (roundId == null) {
            ToolbarUtils.setTitle(this, R.string.new_round)
            loadRoundDefaultValues()
        } else {
            ToolbarUtils.setTitle(this, R.string.edit_round)
            val round = roundDAO.loadRound(roundId!!)
            binding.distance.setItem(round.distance)
            binding.target.setItem(round.target)
            binding.notEditable.visibility = View.GONE
            if (trainingDAO.loadTraining(round.trainingId!!).standardRoundId != null) {
                binding.distanceLayout.visibility = View.GONE
            }
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.setupFabTransform(activity!!, binding.root)
    }

    override fun onSave() {
        navigationController.finish()
        if (roundId == null) {
            val round = onSaveRound()
            navigationController.navigateToRound(round!!)
                .noAnimation()
                .start()
            navigationController.navigateToCreateEnd(round)
        } else {
            onSaveRound()
            activity!!.overridePendingTransition(R.anim.left_in, R.anim.right_out)
        }
    }

    private fun onSaveRound(): Round? {
        val training = trainingDAO.loadTraining(trainingId)

        val round: Round
        if (roundId == null) {
            round = Round()
            round.trainingId = trainingId
            round.shotsPerEnd = binding.arrows.value.toInt()
            round.maxEndCount = null
            round.index = roundDAO.loadRounds(training.id).size
        } else {
            round = roundDAO.loadRound(roundId!!)
        }
        round.distance = binding.distance.selectedItem!!
        round.target = binding.target.selectedItem!!
        if (roundId == null) {
            round.id = roundDAO.insertRound(round)
        } else {
            roundDAO.updateRound(round)
        }
        return round
    }

    private fun updateArrowsLabel() {
        val value = binding.arrows.value.toInt()
        binding.arrowsLabel.text = resources
            .getQuantityString(
                R.plurals.arrow, value, value
            )
    }

    private fun loadRoundDefaultValues() {
        binding.distance.setItem(SettingsManager.distance)
        binding.arrows.value = SettingsManager.shotsPerEnd.toFloat()
        binding.target.setItem(SettingsManager.target)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.target.onActivityResult(requestCode, resultCode, data)
        binding.distance.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val ROUND_ID = "round_id"
    }
}
