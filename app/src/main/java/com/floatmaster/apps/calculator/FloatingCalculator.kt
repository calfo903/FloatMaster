package com.floatmaster.apps.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatmaster.model.FloatingWindow
import kotlin.math.*

@Composable
fun FloatingCalculatorContent(window: FloatingWindow) {
    var display by remember { mutableStateOf("0") }
    var prev by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf<String?>(null) }
    var waitingForOperand by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(listOf<String>()) }

    fun input(d: String) {
        if (waitingForOperand) { display = d; waitingForOperand = false } else display = if (display == "0") d else display + d
    }
    fun inputDot() { if (!display.contains(".")) display += "."; waitingForOperand = false }
    fun clear() { display = "0"; prev = null; op = null; waitingForOperand = false }
    fun backspace() { display = if (display.length <= 1) "0" else display.dropLast(1) }
    fun compute() {
        val cur = display.toDoubleOrNull() ?: return
        val p = prev
        val o = op
        if (p != null && o != null && !waitingForOperand) {
            val res = when (o) {
                "+" -> p + cur
                "−" -> p - cur
                "×" -> p * cur
                "÷" -> if (cur != 0.0) p / cur else Double.NaN
                else -> cur
            }
            history = (history + "$p $o $cur = ${formatResult(res)}").takeLast(5)
            display = formatResult(res)
            prev = null; op = null; waitingForOperand = true
        }
    }
    fun chooseOp(o: String) {
        val cur = display.toDoubleOrNull() ?: return
        if (prev != null && op != null && !waitingForOperand) compute()
        prev = display.toDoubleOrNull()
        op = o
        waitingForOperand = true
    }
    fun applyFunc(f: String) {
        val cur = display.toDoubleOrNull() ?: return
        val res = when (f) {
            "√" -> sqrt(cur)
            "±" -> -cur
            "%" -> cur / 100
            "1/x" -> 1 / cur
            "x²" -> cur * cur
            else -> cur
        }
        display = formatResult(res)
        waitingForOperand = true
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // History
        if (history.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp)) {
                history.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
            }
            Spacer(Modifier.height(8.dp))
        }
        // Display
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), contentAlignment = Alignment.CenterEnd) {
            Text(display, fontSize = 32.sp, fontWeight = FontWeight.Light, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        if (op != null && prev != null) Text("$prev $op", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End))
        Spacer(Modifier.height(12.dp))

        val buttons = listOf(
            listOf("C" to MaterialTheme.colorScheme.errorContainer, "⌫" to MaterialTheme.colorScheme.secondaryContainer, "%" to MaterialTheme.colorScheme.secondaryContainer, "÷" to MaterialTheme.colorScheme.primaryContainer),
            listOf("7" to MaterialTheme.colorScheme.surfaceVariant, "8" to MaterialTheme.colorScheme.surfaceVariant, "9" to MaterialTheme.colorScheme.surfaceVariant, "×" to MaterialTheme.colorScheme.primaryContainer),
            listOf("4" to MaterialTheme.colorScheme.surfaceVariant, "5" to MaterialTheme.colorScheme.surfaceVariant, "6" to MaterialTheme.colorScheme.surfaceVariant, "−" to MaterialTheme.colorScheme.primaryContainer),
            listOf("1" to MaterialTheme.colorScheme.surfaceVariant, "2" to MaterialTheme.colorScheme.surfaceVariant, "3" to MaterialTheme.colorScheme.surfaceVariant, "+" to MaterialTheme.colorScheme.primaryContainer),
            listOf("±" to MaterialTheme.colorScheme.secondaryContainer, "0" to MaterialTheme.colorScheme.surfaceVariant, "." to MaterialTheme.colorScheme.surfaceVariant, "=" to MaterialTheme.colorScheme.primary),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Scientific row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("√", "x²", "1/x").forEach { label ->
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).clickable { applyFunc(label) }, contentAlignment = Alignment.Center) {
                        Text(label, fontWeight = FontWeight.Medium)
                    }
                }
            }
            buttons.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (label, color) ->
                        val isEquals = label == "="
                        Box(
                            modifier = Modifier.weight(1f).height(56.dp).clip(if (isEquals) RoundedCornerShape(28.dp) else CircleShape).background(color).clickable {
                                when (label) {
                                    "C" -> clear()
                                    "⌫" -> backspace()
                                    "=" -> compute()
                                    ".", "," -> inputDot()
                                    "÷", "×", "−", "+" -> chooseOp(label)
                                    "%", "±", "√", "x²", "1/x" -> applyFunc(label)
                                    else -> input(label)
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) { Text(label, fontSize = 20.sp, fontWeight = if (isEquals) FontWeight.Bold else FontWeight.Normal, color = if (isEquals) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        }
    }
}

private fun formatResult(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "Error"
    return if (v % 1 == 0.0) v.toLong().toString() else {
        // trim trailing zeros
        "%.8f".format(v).trimEnd('0').trimEnd('.')
    }
}
