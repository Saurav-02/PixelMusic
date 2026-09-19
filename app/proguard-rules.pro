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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep javax.lang.model classes (often needed by annotation processors or code generation libraries)
-keep class javax.lang.model.** { *; }
-keep interface javax.lang.model.** { *; }

# Keep javax.sound.sampled classes (for audio processing libraries like JFLAC)
-keep class javax.sound.sampled.** { *; }
-keep interface javax.sound.sampled.** { *; }

# Specific rules for JavaPoet if the above is not enough
-keep class com.squareup.javapoet.** { *; }
-keep interface com.squareup.javapoet.** { *; }

# Specific rules for AutoValue if it's directly used or a transitive dependency
# (though usually AutoValue is a compile-time dependency and shouldn't need this)
# -keep class com.google.auto.value.** { *; }
# -keep interface com.google.auto.value.** { *; }

# Rules for TagLib
-keep class com.kyant.taglib.** { *; }

# Rules for JAudioTagger (fallback metadata reader)
-keep class org.jaudiotagger.** { *; }

# [NUEVO] Regla general para mantener metadatos de Kotlin, puede ayudar a R8
-keep class kotlin.Metadata { *; }

# ExoPlayer FFmpeg extension
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.exoplayer.ffmpeg.** { *; }

# ExoPlayer MIDI extension and JSyn synthesizer
-keep class androidx.media3.decoder.midi.** { *; }
-keep class com.jsyn.** { *; }
-keep class com.softsynth.** { *; }
-dontwarn com.jsyn.**
-dontwarn com.softsynth.**

# Mantener clases de datos y sus miembros para evitar que R8 Full elimine campos
-keepclassmembers class com.unshoo.pixelmusic.data.model.** { *; }
-keepclassmembers class com.unshoo.pixelmusic.domain.model.** { *; }

-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*

# Cast framework classes loaded via manifest/reflective entry points.
-keep class com.unshoo.pixelmusic.data.service.cast.CastOptionsProvider { *; }
-keep class * implements com.google.android.gms.cast.framework.OptionsProvider

# Gson generic type capture for backup/restore in release builds.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.unshoo.pixelmusic.data.preferences.PreferenceBackupEntry { *; }
-keep class com.unshoo.pixelmusic.data.backup.model.** { *; }
-keep class com.unshoo.pixelmusic.data.backup.module.** { *; }
# Backup payload entities are part of the persisted .pxpl contract.
-keep class com.unshoo.pixelmusic.data.database.FavoritesEntity { *; }
-keep class com.unshoo.pixelmusic.data.database.SongEngagementEntity { *; }
-keep class com.unshoo.pixelmusic.data.database.LyricsEntity { *; }
-keep class com.unshoo.pixelmusic.data.database.SearchHistoryEntity { *; }
-keep class com.unshoo.pixelmusic.data.database.TransitionRuleEntity { *; }

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.

# [NUEVO] Reglas para solucionar el error de Ktor y R8
-dontwarn java.lang.management.**
-dontwarn reactor.blockhound.**

-dontwarn java.awt.Graphics2D
-dontwarn java.awt.Image
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.ImageWriter
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn javax.sound.sampled.AudioFileFormat$Type
-dontwarn javax.sound.sampled.AudioFileFormat
-dontwarn javax.sound.sampled.AudioFormat$Encoding
-dontwarn javax.sound.sampled.AudioFormat
-dontwarn javax.sound.sampled.AudioInputStream
-dontwarn javax.sound.sampled.UnsupportedAudioFileException
-dontwarn javax.sound.sampled.spi.AudioFileReader
-dontwarn javax.sound.sampled.spi.FormatConversionProvider
-dontwarn javax.swing.filechooser.FileFilter

# Glance Widget
-keep class * extends androidx.glance.appwidget.action.ActionCallback { <init>(); }

# =============================================================================
# TIMBER LOGGING OPTIMIZATION FOR RELEASE BUILDS
# =============================================================================
# Strip VERBOSE and DEBUG log calls entirely from release builds.
# This removes the method calls at bytecode level, eliminating any overhead
# from string concatenation or log message building.

-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# Also strip Timber.Tree methods used by custom trees (belt and suspenders)
-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
    public void i(...);
}

# NOTE: android.util.Log is intentionally NOT stripped here.
# PixelLogger relies on Log.d/Log.i/Log.w/Log.e to reach Logcat.
# Stripping them via -assumenosideeffects removes the calls at bytecode level
# and silently disables the entire logging pipeline in release builds.
# PixelLogger gates its own output on the "verbose_logging_enabled" preference,
# so the cost when the toggle is OFF is one boolean read — no formatting, no IO.

# Missing classes for JSoup Re2j regex delegate and Mozilla Rhino JSON converter bean introspection
-dontwarn com.google.re2j.**
-dontwarn java.beans.**

# Gson serialization keep rules for Explore cache and InnerTube API models
-keep class com.unshoo.pixelmusic.presentation.viewmodel.ExploreCacheModel { *; }
-keep class unshoo.ianshulyadav.pixelmusic.innertube.models.** { *; }
-keep class unshoo.ianshulyadav.pixelmusic.innertube.pages.** { *; }
# Keep InnerTube and YouTube API models safe from R8 minification
-keep class unshoo.ianshulyadav.pixelmusic.innertube.utils.** { *; }
-keep class com.unshoo.pixelmusic.data.model.youtube.** { *; }

-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class com.grack.nanojson.** { *; }


# Mozilla Rhino JS engine references missing javax.script API
-dontwarn javax.script.**
-dontwarn org.mozilla.javascript.engine.**

# Keep desugared library classes (prevent R8 from stripping backported JDK APIs)
-keep class j$.** { *; }

# Protect MusicService and its lifecycle overrides from R8 minification
-keep class com.unshoo.pixelmusic.data.service.MusicService { *; }

# Protect the preferences repository so the background playback toggle state isn't obfuscated
-keep class com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository { *; }

# Suppress R8 missing class warnings for Ktor plugins referenced by Google Generative AI
-dontwarn io.ktor.client.plugins.HttpTimeout**
-dontwarn io.ktor.client.plugins.contentnegotiation.**

# Ignore missing jdk.dynalink classes referenced by Mozilla Rhino JS engine
-dontwarn jdk.dynalink.**

# OkHttp + kotlinx.serialization JSON tree parsing for GeminiAiClient
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
