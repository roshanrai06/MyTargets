-renamesourcefileattribute SourceFile

-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions

-dontnote android.net.http.**
-dontnote org.apache.commons.**
-dontnote org.apache.http.**
-dontwarn sun.misc.Unsafe

# Google Play Services
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-dontnote com.google.android.gms.**

# Ignore duplicate classes in legacy android's http stuff
-dontnote org.apache.http.**
-dontnote android.net.http.**

# Support v4 lib excludes
-keep class android.support.v4.** { *; }
-dontnote android.support.v4.**
-keepattributes Signature

# Workaround for Andorid bug #78377
-keep interface android.support.v4.** { *; }
-keep class !android.support.v7.view.menu.*MenuBuilder*, android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

#Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# MPCharts
-keep class com.github.mikephil.charting.** { *; }
-dontwarn io.realm.**

# OkHttp
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

-keepattributes Annotation
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

#DBFlow
-keep class * extends com.raizlabs.android.dbflow.config.DatabaseHolder { *; }

#Serializeable (e.g.ThreeTenABP's LocalDate)
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Fix OAuth Drive API failure for release builds
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * { @com.google.api.client.util.Key <fields>; }
-keepnames class * implements android.os.Parcelable {
    *;
}

-keepnames class * implements kotlinx.parcelize.Parcelize {
    *;
}
# Parcel reading may lookup/validate the parcel and creator via their
# inner-class relationship. Ensure the attributes are kept and the
# inner/outer relationship is soft pinned. The 'allowshrinking' option
# allows the classes to be removed if unused, but otherwise their attributes
# are retained.
-keepattributes EnclosingClass,InnerClasses
-keep,allowshrinking,allowobfuscation class * implements android.os.Parcelable {}
-keep,allowshrinking,allowobfuscation class * implements android.os.Parcelable$Creator {}

-keep class de.dreier.mytargets.shared.models.db.Training {
    *;
}
# Keep ThreeTenABP classes
-keep class org.threeten.bp.** { *; }
-keepclassmembers class org.threeten.bp.** { *; }

# Keep ThreeTenABP Android System classes (if used)
-keep class org.threeten.bp.zone.AndroidZoneRulesProvider { *; }
-keep class org.threeten.bp.zone.AndroidZoneRules { *; }

# Additional ProGuard rules for ThreeTenABP
-keepclassmembers enum org.threeten.bp.temporal.ChronoField {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class org.threeten.bp.zone.TzdbZoneRulesProvider {
    public static **[] getAvailableRulesIds();
    public static **[] getAvailableRules();
}
-keep class org.threeten.bp.zone.*

# Add other rules as needed for specific classes used in your project

