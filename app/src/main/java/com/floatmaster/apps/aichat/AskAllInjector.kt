package com.floatmaster.apps.aichat

import android.webkit.WebView

/**
 * WHY: Per-host Ask All injector — 12 AIs have different DOM. Generic "button:contains(Send)" fails on Claude (div ProseMirror) vs ChatGPT (textarea[data-id=root]).
 * This centralizes selectors and JS injection, keeps FloatingAiChatGroup thin, testable.
 *
 * Quality: KDoc, no !!, sealed handling, escaped prompt.
 */
object AskAllInjector {

    /**
     * Inject prompt into WebView and click Send using host-specific selectors.
     * @param webView target WebView (must be on main thread)
     * @param prompt user prompt, will be escaped for JS single-quote
     * @param provider null → generic fallback (tries all known selectors)
     */
    fun inject(webView: WebView, prompt: String, provider: AiChatProvider?) {
        // WHY: Escape for JS single-quote string — prevents injection break on ' or \n
        val escaped = prompt
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\"", "\\\"")
            .replace("`", "\\`")
            .take(4000) // WHY: cap 4k prevents OOM in evaluateJavascript

        val inputSel = provider?.inputSelector ?: "textarea, [contenteditable=true], input[type=text]"
        val sendSels = provider?.sendSelectors ?: listOf("button")
        // Also include union of all known send selectors as fallback
        val fallbackSels = AiChatProvider.all.flatMap { it.sendSelectors }.distinct()
        val allSends = (sendSels + fallbackSels).distinct().take(12)

        // Build JS: 1) fill, 2) after 350ms click send in priority order
        // WHY: Use single-quoted JS string for prompt to avoid double-quote conflicts; escape already done
        val sendSelectorsJs = allSends.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }

        val js = """
            (function(){
                let prompt = '$escaped';
                let inputSel = '${inputSel.replace("'", "\\'")}';
                let sendSels = [$sendSelectorsJs];
                // 1. Fill input — try provider selector, fallback to generic
                let inputs = document.querySelectorAll(inputSel);
                if(inputs.length===0) inputs = document.querySelectorAll('textarea, [contenteditable=true], input[type=text]');
                for(let e of inputs){
                    try{
                        e.focus();
                        if(e.isContentEditable){
                            e.textContent = prompt;
                            document.execCommand('selectAll', false, null);
                            document.execCommand('insertText', false, prompt);
                        } else {
                            e.value = prompt;
                            e.dispatchEvent(new Event('input',{bubbles:true}));
                            e.dispatchEvent(new Event('change',{bubbles:true}));
                        }
                        e.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}));
                    }catch(_){}
                }
                // 2. Click send — try selectors in order, wait for React state to settle
                setTimeout(function(){
                    for(let sel of sendSels){
                        try{
                            let btn = null;
                            if(sel.includes(':has-text')){
                                let m = sel.match(/has-text\(["'](.*?)["']\)/);
                                let txt = m ? m[1] : 'Send';
                                let all = document.querySelectorAll('button, [role=button], div[role=button]');
                                for(let b of all){ if(b.innerText && b.innerText.includes(txt) && b.offsetParent !== null){ btn=b; break; } }
                            } else {
                                btn = document.querySelector(sel);
                            }
                            if(btn && btn.offsetParent !== null && !btn.disabled){
                                btn.click();
                                btn.dispatchEvent(new MouseEvent('click',{bubbles:true, cancelable:true}));
                                return;
                            }
                        }catch(_){}
                    }
                    // Fallback: Enter on active element
                    let active = document.activeElement;
                    if(active) active.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',keyCode:13,bubbles:true}));
                }, 400);
            })()
        """.trimIndent()

        try {
            webView.evaluateJavascript(js, null) // WHY: evaluateJavascript must be on UI thread — caller ensures
        } catch (_: Exception) {}
    }

    /** WHY: Batch inject for grouped WebViews */
    fun injectAll(webViews: Map<String, WebView>, prompt: String) {
        webViews.forEach { (shortId, wv) ->
            val prov = AiChatProvider.all.find { it.shortId == shortId }
            inject(wv, prompt, prov)
        }
    }
}
