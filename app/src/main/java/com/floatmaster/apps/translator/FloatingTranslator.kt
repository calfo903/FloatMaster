package com.floatmaster.apps.translator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MyMemoryApi {
    @GET("get")
    suspend fun translate(@Query("q") text: String, @Query("langpair") langpair: String): MyMemoryResponse
}
data class MyMemoryResponse(val responseData: ResponseData?)
data class ResponseData(val translatedText: String?)

@Composable
fun FloatingTranslatorContent(window: FloatingWindow) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("en") }
    var target by remember { mutableStateOf("es") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val api = remember {
        Retrofit.Builder().baseUrl("https://api.mymemory.translated.net/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(MyMemoryApi::class.java)
    }
    val languages = listOf("en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German", "ar" to "Arabic", "sw" to "Swahili", "hi" to "Hindi", "zh" to "Chinese", "ja" to "Japanese", "pt" to "Portuguese", "tr" to "Turkish")

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            DropdownLang("From", source, languages) { source = it }
            IconButton(onClick = { val t = source; source = target; target = t; val tmp = input; input = output; output = tmp }) { Icon(Icons.Default.SwapHoriz, null) }
            DropdownLang("To", target, languages) { target = it }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("Enter text") }, label = { Text(languages.find { it.first == source }?.second ?: source) })
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (input.isBlank()) return@Button
                scope.launch {
                    loading = true
                    try {
                        val res = api.translate(input, "$source|$target")
                        output = res.responseData?.translatedText ?: "No translation"
                    } catch (e: Exception) {
                        output = "Error: ${e.message}\n(Fallback: try Google Translate web)"
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Translate, null)
            Spacer(Modifier.width(8.dp))
            Text("Translate")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = output, onValueChange = {}, modifier = Modifier.fillMaxWidth().height(120.dp), readOnly = true, label = { Text(languages.find { it.first == target }?.second ?: target) }, placeholder = { Text("Translation") })
        Spacer(Modifier.height(8.dp))
        Text("Powered by MyMemory free API (1000 req/day). For more languages, the WebView fallback loads translate.google.com.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun DropdownLang(label: String, selected: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: ${options.find { it.first == selected }?.second ?: selected}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, name) ->
                DropdownMenuItem(text = { Text("$name ($code)") }, onClick = { onSelected(code); expanded = false })
            }
        }
    }
}
