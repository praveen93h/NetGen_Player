# ProGuard rules for NextGen Media Player

# Keep Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room entities
-keep class com.nextgen.player.data.local.entity.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-dontwarn coil.**

# Network libraries
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class net.engio.** { *; }
-dontwarn net.engio.**
-keep class com.jcraft.** { *; }
-dontwarn com.jcraft.**
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# smbj / mbassador missing classes
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**

# Keep app classes
-keep class com.nextgen.player.** { *; }
