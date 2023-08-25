package de.dreier.mytargets.features.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import de.dreier.mytargets.R
import de.dreier.mytargets.base.fragments.FragmentBase


class ShopFragment : FragmentBase() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shop, container, false)
        val webView = view.findViewById<WebView>(R.id.webViewShop)
        val webSettings = webView.settings
        webView.loadUrl("https://mantisarchery.com?utm_source=android&utm_medium=app&utm_app=MyTarget")

        // Enable Javascript
        webSettings.javaScriptEnabled = true
        // Force links and redirects to open in the WebView instead of in a browser
        webView.webViewClient = WebViewClient()
        webView.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack() // Navigate back to previous web page if there is one
                webView.scrollTo(0, 0) // Scroll web-view back to top of previous page
            }
            true
        }
        return view


    }


}