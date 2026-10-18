# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.interviewprep.**$$serializer { *; }
-keepclassmembers class com.interviewprep.** { *** Companion; }
-keepclasseswithmembers class com.interviewprep.** { kotlinx.serialization.KSerializer serializer(...); }
# Chaquopy
-keep class com.chaquo.python.** { *; }
