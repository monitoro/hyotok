# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Data Models (Keep all data classes for Gson reflection)
-keep class com.silverpixelism.hyotok.data.** { *; }

# Open-Meteo specific if needed (but covered by data.**)
