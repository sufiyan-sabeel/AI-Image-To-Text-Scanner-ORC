package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.documentDao()

    val documents = dao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentExtractedText = MutableStateFlow("")
    val currentExtractedText: StateFlow<String> = _currentExtractedText

    private val _detectedLanguage = MutableStateFlow("Unknown")
    val detectedLanguage: StateFlow<String> = _detectedLanguage

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val languageIdentifier = LanguageIdentification.getClient()

    private fun detectLanguage(text: String) {
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    _detectedLanguage.value = "Unknown"
                } else {
                    _detectedLanguage.value = languageCode.uppercase()
                }
            }
            .addOnFailureListener {
                _detectedLanguage.value = "Unknown"
            }
    }

    fun processImage(bitmap: Bitmap, isHandwriting: Boolean = false) {
        _isProcessing.value = true
        val image = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                _currentExtractedText.value = visionText.text
                detectLanguage(visionText.text)
                _isProcessing.value = false
            }
            .addOnFailureListener { e ->
                Log.e("ScannerViewModel", "OCR failed", e)
                _currentExtractedText.value = "Unable to recognize text. Try a clearer image."
                _isProcessing.value = false
            }
    }

    fun processMultipleImagesFromUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val combinedText = StringBuilder()
                for (uri in uris.take(20)) { // limit to 20 pages
                    val image = InputImage.fromFilePath(context, uri)
                    val visionText = recognizer.process(image).await()
                    combinedText.append(visionText.text).append("\n\n--- Page Break ---\n\n")
                }
                _currentExtractedText.value = combinedText.toString().trim()
                detectLanguage(_currentExtractedText.value)
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Batch OCR failed", e)
                _currentExtractedText.value = "Failed to process multiple images."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processImageFromUri(context: Context, uri: Uri) {
        processMultipleImagesFromUris(context, listOf(uri))
    }

    fun processPdfFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val combinedText = StringBuilder()
                    
                    val pageCount = minOf(renderer.pageCount, 20)
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2, // High resolution for OCR
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val image = InputImage.fromBitmap(bitmap, 0)
                        val visionText = recognizer.process(image).await()
                        combinedText.append(visionText.text).append("\n\n--- Page Break ---\n\n")
                    }
                    
                    renderer.close()
                    pfd.close()

                    if (combinedText.isNotEmpty()) {
                        _currentExtractedText.value = combinedText.toString().trim()
                        detectLanguage(_currentExtractedText.value)
                    } else {
                        _currentExtractedText.value = "PDF has no readable pages."
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to process PDF", e)
                _currentExtractedText.value = "Failed to process PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportToPdf(context: Context, text: String, title: String) {
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                var page = document.startPage(pageInfo)
                var canvas = page.canvas
                val paint = android.graphics.Paint().apply {
                    color = AndroidColor.BLACK
                    textSize = 12f
                }
                
                var yOffset = 50f
                val margin = 50f
                val maxWidth = pageInfo.pageWidth - (2 * margin)
                
                val lines = text.split("\n")
                for (line in lines) {
                    // Simple word wrap (very basic)
                    var currentLine = line
                    while (currentLine.isNotEmpty()) {
                        val charsToDraw = paint.breakText(currentLine, true, maxWidth, null)
                        val drawString = currentLine.substring(0, charsToDraw)
                        canvas.drawText(drawString, margin, yOffset, paint)
                        yOffset += paint.descent() - paint.ascent()
                        
                        currentLine = currentLine.substring(charsToDraw)
                        
                        if (yOffset > pageInfo.pageHeight - margin) {
                            document.finishPage(page)
                            page = document.startPage(pageInfo)
                            canvas = page.canvas
                            yOffset = 50f
                        }
                    }
                }
                document.finishPage(page)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "$title.pdf")
                val fos = FileOutputStream(file)
                document.writeTo(fos)
                document.close()
                fos.close()
                Log.d("ScannerViewModel", "PDF Saved to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to create PDF", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateExtractedText(text: String) {
        _currentExtractedText.value = text
    }

    fun translateText(text: String, targetLanguage: String) {
        _isProcessing.value = true
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguage)
            .build()
        val translator = Translation.getClient(options)
        
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        _translatedText.value = translatedText
                        _isProcessing.value = false
                    }
                    .addOnFailureListener {
                        _translatedText.value = "Translation failed."
                        _isProcessing.value = false
                    }
            }
            .addOnFailureListener {
                _translatedText.value = "Failed to download language model."
                _isProcessing.value = false
            }
    }

    fun saveDocument(title: String, type: String = "Scan") {
        viewModelScope.launch {
            val content = _currentExtractedText.value
            if (content.isNotBlank()) {
                val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }
                val doc = DocumentEntity(
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    wordCount = wordCount
                )
                dao.insertDocument(doc)
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            dao.deleteDocument(doc)
        }
    }

    fun enhanceTextWithAI(originalText: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val prompt = "You are an AI assistant in a premium OCR application. Please enhance the following text extracted from an image. Fix spelling and grammar, format it clearly, and preserve the original meaning. Text:\n$originalText"
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = prompt)))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = "You are a helpful OCR formatting assistant.")))
                )
                
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val newText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (newText != null) {
                    _currentExtractedText.value = newText
                } else {
                    Log.e("ScannerViewModel", "Gemini returned empty text")
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to enhance text", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
