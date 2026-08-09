package com.floatmaster.apps.aichat

import android.webkit.WebView
import org.json.JSONObject

/**
 * WHY: Centralize provider-specific DOM automation and serialize every string as JSON before embedding it in JS.
 * The injector has no native JS bridge, caps prompt size, and tolerates DOM drift without throwing into the UI.
 */
object AskAllInjector {
    fun inject(webView: WebView, prompt: String, provider: AiChatProvider?) {
        val promptJs = JSONObject.quote(prompt.take(4000))
        val inputSelectorJs = JSONObject.quote(provider?.inputSelector ?: "textarea, [contenteditable=true], input[type=text]")
        val sendSelectors = (provider?.sendSelectors.orEmpty() + AiChatProvider.all.flatMap { it.sendSelectors })
            .distinct()
            .take(12)
        val sendSelectorsJs = sendSelectors.joinToString(",") { JSONObject.quote(it) }

        val js = """
            (function(){
                const prompt = $promptJs;
                const inputSel = $inputSelectorJs;
                const sendSels = [$sendSelectorsJs];
                try {
                    let inputs = document.querySelectorAll(inputSel);
                    if (inputs.length === 0) inputs = document.querySelectorAll('textarea, [contenteditable=true], input[type=text]');
                    for (const e of inputs) {
                        try {
                            e.focus();
                            if (e.isContentEditable) {
                                e.textContent = prompt;
                                e.dispatchEvent(new InputEvent('input', {bubbles:true, inputType:'insertText', data:prompt}));
                            } else {
                                e.value = prompt;
                                e.dispatchEvent(new Event('input', {bubbles:true}));
                                e.dispatchEvent(new Event('change', {bubbles:true}));
                            }
                        } catch (_) {}
                    }
                    setTimeout(function(){
                        for (const sel of sendSels) {
                            try {
                                const btn = document.querySelector(sel);
                                if (btn && btn.offsetParent !== null && !btn.disabled) {
                                    btn.click();
                                    return;
                                }
                            } catch (_) {}
                        }
                        const buttons = document.querySelectorAll('button, [role=button]');
                        for (const btn of buttons) {
                            try {
                                const text = (btn.innerText || '').trim().toLowerCase();
                                if (text === 'send' && btn.offsetParent !== null && !btn.disabled) {
                                    btn.click();
                                    return;
                                }
                            } catch (_) {}
                        }
                    }, 400);
                } catch (_) {}
            })();
        """.trimIndent()

        runCatching { webView.evaluateJavascript(js, null) }
    }

    fun injectAll(webViews: Map<String, WebView>, prompt: String) {
        webViews.forEach { (shortId, webView) ->
            val provider = AiChatProvider.all.firstOrNull { it.shortId == shortId }
            inject(webView, prompt, provider)
        }
    }
}
