# ProGuard rules for G365Calendar

# MSAL
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

# Moshi
-keep class com.g365calendar.data.model.** { *; }
-keepclassmembers class com.g365calendar.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes used for JSON serialization
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
