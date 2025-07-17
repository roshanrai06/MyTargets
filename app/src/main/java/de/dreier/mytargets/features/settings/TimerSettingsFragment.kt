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

package de.dreier.mytargets.features.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.dreier.mytargets.R
import de.dreier.mytargets.app.ApplicationInstance


class TimerSettingsFragment : SettingsFragmentBase() {



    override fun updateItemSummaries() {
        val settings = SettingsManager.timerSettings
        setSecondsSummary(SettingsManager.KEY_TIMER_WAIT_TIME, settings.waitTime)
        setSecondsSummary(SettingsManager.KEY_TIMER_SHOOT_TIME, settings.shootTime)
        setSecondsSummary(SettingsManager.KEY_TIMER_WARN_TIME, settings.warnTime)
    }

    private fun setSecondsSummary(key: String, value: Int) {
        setSummary(key, resources.getQuantityString(R.plurals.second, value, value))
    }
}
