# Keep rules for UnlockVocab
-keep class com.huy.unlockvocab.data.** { *; }
-keep class com.huy.unlockvocab.domain.** { *; }
-keep class com.huy.unlockvocab.security.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn org.json.**
