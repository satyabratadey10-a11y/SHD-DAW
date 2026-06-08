-keepclasseswithmembernames class * { native <methods>; }
-keep class com.example.ui.NativeAudioInterface { *; }
-keep class com.example.ui.DawStateViewModel { *; }

# Serialization data classes mapping to custom .msp schemas
-keep class com.example.ui.MidiNote { *; }
-keep class com.example.ui.Clip { *; }
-keep class com.example.ui.SnapMode { *; }
-keep class com.example.** { *; }
