package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val extractedText by viewModel.currentExtractedText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var targetLanguage by remember { mutableStateOf("fr") }
    var sourceLanguage by remember { mutableStateOf("en") }

    LaunchedEffect(targetLanguage) {
        if (extractedText.isNotBlank()) {
            viewModel.translateText(extractedText, targetLanguage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translation", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            // Language selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { /* TODO select source */ }, modifier = Modifier.weight(1f)) {
                    Text("Auto Detect", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
                
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                var expanded by remember { mutableStateOf(false) }
                val languageMap = mapOf("fr" to "French", "es" to "Spanish", "de" to "German")
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { expanded = true }) {
                        Text(languageMap[targetLanguage] ?: "Select", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("French") },
                            onClick = { targetLanguage = "fr"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Spanish") },
                            onClick = { targetLanguage = "es"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("German") },
                            onClick = { targetLanguage = "de"; expanded = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Source Text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Source Text", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translated Text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Column {
                            Text("Translation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = translatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
