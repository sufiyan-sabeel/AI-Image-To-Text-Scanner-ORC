package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    onNavigateToSlides: () -> Unit
) {
    val extractedText by viewModel.currentExtractedText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isGeneratingSlides by viewModel.isGeneratingSlides.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    var editableText by remember { mutableStateOf(extractedText) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfTitle by remember { mutableStateOf("") }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<java.io.File?>(null) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    // Update local state when viewmodel state changes (initial load or AI enhancement)
    LaunchedEffect(extractedText) {
        if (extractedText != editableText) {
            editableText = extractedText
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extracted Document", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.enhanceTextWithAI(editableText) },
                        enabled = !isProcessing && !isGeneratingSlides && editableText.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Enhance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            viewModel.updateExtractedText(editableText)
                            viewModel.generateSlidesFromText(editableText) {
                                onNavigateToSlides()
                            }
                        },
                        enabled = !isProcessing && !isGeneratingSlides && editableText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Slideshow,
                            contentDescription = "Generate Slides",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Feature Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export Professional PDF
                        Button(
                            onClick = {
                                val firstLine = editableText.lines().firstOrNull { it.isNotBlank() }?.take(25) ?: "Document"
                                pdfTitle = firstLine.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().ifBlank { "Scanned_Document" }
                                generatedPdfFile = null
                                showPdfDialog = true
                            },
                            enabled = !isProcessing && editableText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Generate Slides
                        FilledTonalButton(
                            onClick = {
                                viewModel.updateExtractedText(editableText)
                                viewModel.generateSlidesFromText(editableText) {
                                    onNavigateToSlides()
                                }
                            },
                            enabled = !isProcessing && !isGeneratingSlides && editableText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Slides", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Copy
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(editableText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isProcessing && editableText.isNotBlank()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }

                            // Share Plain Text
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, editableText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Scanned Text")
                                    context.startActivity(shareIntent)
                                },
                                enabled = !isProcessing && editableText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Text")
                            }

                            // Save to local library
                            IconButton(
                                onClick = {
                                    viewModel.updateExtractedText(editableText)
                                    val firstLine = editableText.lines().firstOrNull { it.isNotBlank() }?.take(25) ?: "Scanned Doc"
                                    viewModel.saveDocument(firstLine)
                                },
                                enabled = !isProcessing && editableText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }

                        // Translate button
                        OutlinedButton(
                            onClick = {
                                viewModel.updateExtractedText(editableText)
                                onNavigateToTranslate()
                            },
                            enabled = !isProcessing && editableText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isProcessing || isGeneratingSlides) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        if (isGeneratingSlides) "Generating Presentation Slides..." else "Processing document...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Applying formatting and structure",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Document Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem("Words", editableText.split("\\s+".toRegex()).count { it.isNotEmpty() }.toString())
                        StatItem("Characters", editableText.length.toString())
                        StatItem("Language", detectedLanguage.uppercase())
                        StatItem("Format", "A4 / Slides")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text Editor Area
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        TextField(
                            value = editableText,
                            onValueChange = { editableText = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Extracted text will appear here. You can edit, enhance with AI, export to PDF, or generate presentation slides.") }
                        )
                    }
                }
            }
        }
    }

    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGeneratingPdf) showPdfDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Export Document PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.animateContentSize()) {
                    Text(
                        "Generate a professional PDF with document metadata, clean typography, headers, and page numbers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (generatedPdfFile != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "✓ Professional PDF Generated!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.openLastGeneratedPdf(context)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Open PDF", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.shareLastGeneratedPdf(context, pdfTitle)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Share PDF", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (generatedPdfFile == null) {
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            viewModel.exportDocumentPdf(
                                context = context,
                                text = editableText,
                                title = pdfTitle.ifBlank { "Scanned_Document" }
                            ) { file ->
                                isGeneratingPdf = false
                                generatedPdfFile = file
                            }
                        },
                        enabled = !isGeneratingPdf,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Text("Generate PDF")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            showPdfDialog = false
                            generatedPdfFile = null
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPdfDialog = false
                        generatedPdfFile = null
                    },
                    enabled = !isGeneratingPdf
                ) {
                    Text(if (generatedPdfFile != null) "Close" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
