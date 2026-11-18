# Keep rules for UnlockVocab
# Allow R8 to obfuscate data, domain, and security packages while preserving enum names persisted to SQLite/SharedPreferences.
-keep enum com.huy.unlockvocab.data.db.PosTag { *; }
-keep enum com.huy.unlockvocab.data.db.TriggerMode { *; }
-keep enum com.huy.unlockvocab.data.db.WordSelectionPolicy { *; }
-keep enum com.huy.unlockvocab.data.db.ReviewOutcome { *; }
-keep enum com.huy.unlockvocab.data.db.CefrLevel { *; }
-keep enum com.huy.unlockvocab.data.db.QuizQuestionStyle { *; }
-keep enum com.huy.unlockvocab.data.db.ResolvedPromptType { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn org.json.**
