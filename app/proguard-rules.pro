# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===========================================================================
# Retrofit 2.11.0
# ===========================================================================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keep interface com.example.v_sat_compass.data.api.** { *; }

# ===========================================================================
# Gson 2.11.0
# ===========================================================================
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.v_sat_compass.data.model.** { *; }

# ===========================================================================
# Glide 4.16.0
# ===========================================================================
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# ===========================================================================
# OkHttp 4.12.0
# ===========================================================================
-dontwarn okhttp3.internal.platform.**

# ===========================================================================
# AndroidX @Keep annotation
# ===========================================================================
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
