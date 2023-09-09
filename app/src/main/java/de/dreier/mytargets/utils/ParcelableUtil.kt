package de.dreier.mytargets.utils

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

inline fun <reified T : Parcelable> Intent.parcelableExtra(intent: Intent, key: String): T? {
    return IntentCompat.getParcelableExtra(intent, key, T::class.java)

}

inline fun <reified T : Parcelable> Bundle.parcelable(bundle: Bundle, key: String): T? {
    return BundleCompat.getParcelable(bundle, key, T::class.java)

}