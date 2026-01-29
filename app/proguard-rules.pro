# ProGuard rules for webview-app

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep application classes
-keep class com.example.webviewapp.** { *; }
