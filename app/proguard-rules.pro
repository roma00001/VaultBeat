# YoutubeDL-android needs to keep its JNI and classes
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Coil
-keep class coil3.** { *; }

# Media3 (needed if using specialized components)
-keep class androidx.media3.** { *; }
