package com.example.v_sat_compass.util;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public final class KaTeXHelper {

    private KaTeXHelper() {}

    public static boolean containsMath(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.contains("$")
                || text.contains("\\[")
                || text.contains("\\(")
                || text.contains("\\frac")
                || text.contains("\\sqrt");
    }

    public static String escapeForHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }

    public static String buildKaTeXHtml(String rawText) {
        String body = escapeForHtml(rawText);
        // ponytail: embedded WebView template uses local assets; upgrade to dynamic light/dark theme styling when system dark mode supported
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
                + "<link rel=\"stylesheet\" href=\"file:///android_asset/katex/katex.min.css\">"
                + "<script src=\"file:///android_asset/katex/katex.min.js\"></script>"
                + "<script src=\"file:///android_asset/katex/auto-render.min.js\"></script>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 15px; line-height: 1.6; color: #212121; margin: 0; padding: 0; background: transparent; word-wrap: break-word; }"
                + ".katex-display { margin: 8px 0; overflow-x: auto; overflow-y: hidden; }"
                + "</style></head><body>"
                + "<div id=\"math-content\">" + body + "</div>"
                + "<script>"
                + "document.addEventListener('DOMContentLoaded', function() {"
                + "  renderMathInElement(document.getElementById('math-content'), {"
                + "    delimiters: ["
                + "      {left: '$$', right: '$$', display: true},"
                + "      {left: '$', right: '$', display: false},"
                + "      {left: '\\\\[', right: '\\\\]', display: true},"
                + "      {left: '\\\\(', right: '\\\\)', display: false}"
                + "    ],"
                + "    throwOnError: false"
                + "  });"
                + "});"
                + "</script></body></html>";
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void renderOrText(WebView webView, TextView textView, String text) {
        if (textView == null) return;
        if (webView != null && containsMath(text)) {
            textView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            renderMath(webView, text);
        } else {
            if (webView != null) {
                webView.setVisibility(View.GONE);
            }
            textView.setVisibility(View.VISIBLE);
            textView.setText(text != null ? text : "");
        }
    }

    public static void renderMath(WebView webView, String rawText) {
        if (webView == null) return;
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        String html = buildKaTeXHtml(rawText);
        webView.loadDataWithBaseURL("file:///android_asset/katex/", html, "text/html", "UTF-8", null);
    }
}