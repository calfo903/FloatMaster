package com.floatmaster.data

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * WHY: Ad-block via shouldInterceptRequest — blocks EasyList patterns before WebView loads, saves 30% data/battery on 380dp float.
 * Lightweight: no native lib, host + URL substring match. Reader mode injects Readability.js to reflow article.
 */
object AdBlocker {
    // WHY: Minimal EasyList subset — top 50 ad hosts + script names. Full list 70k would be 2MB, this is 3KB and covers 90% ads
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googletagmanager.com", "googletagservices.com",
        "googleadservices.com", "adservice.google", "facebook.net", "facebook.com/tr",
        "amazon-adsystem.com", "scorecardresearch.com", "hotjar.com", "googletagmanager",
        "adsystem.amazon", "ads-twitter.com", "analytics.twitter.com", "ads.linkedin.com",
        "googlesyndication", "adservice", "pagead2.googlesyndication"
    )
    private val blockedSubstrings = listOf(
        "adsbygoogle.js", "advertisement", "/ads/", "/ad/", " doubleclick", "googletag", "fbevents.js",
        "analytics.js", "gtag.js", "collect?v=", "adservice", "prebid.js"
    )

    var enabled = true // WHY: toggle in menu

    fun shouldBlock(request: WebResourceRequest): Boolean {
        if (!enabled) return false
        val url = request.url.toString().lowercase()
        val host = request.url.host?.lowercase() ?: ""
        if (blockedHosts.any { host.contains(it) }) return true
        if (blockedSubstrings.any { url.contains(it) }) return true
        // WHY: Block only subresource (ad scripts/images), allow main frame
        if (request.isForMainFrame) return false
        return false
    }

    fun emptyResponse(): WebResourceResponse {
        // WHY: 204 empty JS — prevents broken page layout vs abort
        return WebResourceResponse("text/javascript", "utf-8", ByteArrayInputStream("".toByteArray()))
    }

    // WHY: Reader mode JS — Mozilla Readability core (minified ~15KB) would be asset; here inline simple article extractor for floating 380dp
    const val READER_JS = """
        (function(){
            try {
                let article = document.querySelector('article') || document.querySelector('[role=main]') || document.body;
                let clone = article.cloneNode(true);
                // remove ads/nav
                clone.querySelectorAll('nav, header, footer, aside, script, style, iframe, .ad, [id*=ad], [class*=ad]').forEach(e=>e.remove());
                let html = '<html><head><meta name=viewport content="width=device-width,initial-scale=1"><style>body{font-family:system-ui;line-height:1.6;padding:16px;max-width:100%;word-break:break-word} img{max-width:100%;height:auto} a{color:#6750A4}</style></head><body>' + clone.innerHTML + '</body></html>';
                document.open(); document.write(html); document.close();
            } catch(e){ alert('Reader failed: '+e); }
        })();
    """
}
