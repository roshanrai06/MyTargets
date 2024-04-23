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

package de.dreier.mytargets.features.settings.about

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import de.dreier.mytargets.BuildConfig
import de.dreier.mytargets.R
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutFragment : Fragment() {

    private val version: String
        get() = getString(
            R.string.version,
            BuildConfig.VERSION_NAME
        ) + " (${BuildConfig.VERSION_CODE})"



    private val betaTesterElement: Element
        get() = WebElement(R.string.test_beta, R.drawable.about_icon_beta_test, URL_PLAY_STORE)

    private val slackElement: Element
        get() = WebElement(
            R.string.join_on_slack, R.drawable.about_icon_slack,
            URL_SLACK
        )


    private val shareElement: Element
        get() {
            val shareElement =
                Element(getString(R.string.share_with_friends), R.drawable.about_icon_share)
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, URL_PLAY_STORE)
            sendIntent.type = "text/plain"
            shareElement.intent =
                Intent.createChooser(sendIntent, getString(R.string.share_with_friends))
            return shareElement
        }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return AboutPage(context)
            .isRTL(false)
            .setImage(R.drawable.product_logo_144dp)
            .setDescription(getString(R.string.my_targets) + "\n" + version)
            .addGroup(getString(R.string.contribute))
            .addItem(shareElement)
            .addItem(betaTesterElement)
            .addGroup(getString(R.string.connect))
            .addPlayStore("de.dreier.mytargets")
            .addItem(slackElement)
            .addGroup(getString(R.string.special_thanks_to))
            .addItem(Element(getString(R.string.all_beta_testers), null))
            .addItem(
                Element(
                    getString(R.string.all_translators)
                            + "\n" + getString(R.string.translators), null
                )
            )
            .addGroup("Handicap Calculations (" + getString(R.string.handicap_symbol) + ")")
            .addItem(Element(
                    "Original Handicap Calculation and Tables" + "\n" +
                    "   David Lane" + "\n" +
                    "\n" +
                    "Online Tables Creator and Demystification" + "\n" +
                    "   Jack Atkinson" + "\n" +
                    "\n" +
                    "Java/Kotlin implementation" + "\n" +
                    "   Jez McKinley" + "\n" +
                    "\n" +
                    "See in-app help pages for detailed information",
                    null)
            )
            .create()
    }

    private inner class WebElement internal constructor(
        @StringRes title: Int, icon: Int?,
        url: String
    ) : Element(getString(title), icon) {
        init {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = url.toUri()
            setIntent(intent)
        }
    }

    companion object {
        private const val URL_SLACK =
            "https://join.slack.com/t/mytargets/shared_invite/enQtNjk2NTE0MzU5NzE0LTc3NjAwYmZiNTcxMDA1NTI0M2UzYWY4ZGQwMjVhYjEyODQ0MDE2MjlhZjZiZTUwODg2YTE5YjhkN2FmZTQ2Njc"
        private const val URL_PLAY_STORE =
            "http://play.google.com/store/apps/details?id=de.dreier.mytargets"
    }
}
