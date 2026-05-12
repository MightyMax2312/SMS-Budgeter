# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.budgettracker.data.local.entity.** { *; }