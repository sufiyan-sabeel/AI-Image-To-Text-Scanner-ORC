package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.LanguageCatalog
import com.example.data.LanguageItem
import com.example.data.SlideItem
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.util.PdfExportHelper
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
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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

    private val _isGeneratingSlides = MutableStateFlow(false)
    val isGeneratingSlides: StateFlow<Boolean> = _isGeneratingSlides

    private val _slideDeck = MutableStateFlow<List<SlideItem>>(emptyList())
    val slideDeck: StateFlow<List<SlideItem>> = _slideDeck

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    val supportedLanguages: Map<String, String> = LanguageCatalog.allLanguages.associate { it.code to it.name }
    val allLanguagesList: List<LanguageItem> = LanguageCatalog.allLanguages
    val indianLanguagesList: List<LanguageItem> = LanguageCatalog.indianLanguages
    val foreignLanguagesList: List<LanguageItem> = LanguageCatalog.foreignLanguages

    private val _selectedSourceLanguage = MutableStateFlow("auto")
    val selectedSourceLanguage: StateFlow<String> = _selectedSourceLanguage

    private val _selectedTargetLanguage = MutableStateFlow("hi")
    val selectedTargetLanguage: StateFlow<String> = _selectedTargetLanguage
    val targetLanguage: StateFlow<String> = _selectedTargetLanguage

    private val _lastGeneratedPdf = MutableStateFlow<File?>(null)
    val lastGeneratedPdf: StateFlow<File?> = _lastGeneratedPdf

    fun setSourceLanguage(languageCode: String) {
        _selectedSourceLanguage.value = languageCode
    }

    fun setSelectedTargetLanguage(languageCode: String) {
        _selectedTargetLanguage.value = languageCode
    }

    fun setTargetLanguage(languageCode: String) {
        setSelectedTargetLanguage(languageCode)
    }

    fun swapLanguages() {
        val currentSrc = _selectedSourceLanguage.value
        val currentTgt = _selectedTargetLanguage.value
        val effectiveSrc = if (currentSrc == "auto") {
            if (_detectedLanguage.value.length >= 2 && _detectedLanguage.value != "Unknown") {
                _detectedLanguage.value.substring(0, 2)
            } else "en"
        } else {
            currentSrc
        }

        _selectedSourceLanguage.value = currentTgt
        _selectedTargetLanguage.value = effectiveSrc

        val currentExtracted = _currentExtractedText.value
        val currentTrans = _translatedText.value
        if (currentTrans.isNotBlank()) {
            _currentExtractedText.value = currentTrans
            _translatedText.value = currentExtracted
        }
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val languageIdentifier = LanguageIdentification.getClient()

    fun processImage(bitmap: Bitmap, onComplete: (() -> Unit)? = null) {
        _isProcessing.value = true
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                _currentExtractedText.value = visionText.text
                identifyLanguage(visionText.text)
                autoTranslate(visionText.text, _selectedTargetLanguage.value)
                _isProcessing.value = false
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("ScannerViewModel", "Text recognition failed", e)
                _currentExtractedText.value = "Failed to recognize text."
                _isProcessing.value = false
                onComplete?.invoke()
            }
    }

    fun processImageUri(context: Context, uri: Uri, onComplete: (() -> Unit)? = null) {
        _isProcessing.value = true
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    _currentExtractedText.value = visionText.text
                    identifyLanguage(visionText.text)
                    autoTranslate(visionText.text, _selectedTargetLanguage.value)
                    _isProcessing.value = false
                    onComplete?.invoke()
                }
                .addOnFailureListener { e ->
                    Log.e("ScannerViewModel", "Text recognition failed", e)
                    _currentExtractedText.value = "Failed to recognize text."
                    _isProcessing.value = false
                    onComplete?.invoke()
                }
        } catch (e: Exception) {
            Log.e("ScannerViewModel", "Failed to open image URI", e)
            _isProcessing.value = false
            onComplete?.invoke()
        }
    }

    private fun identifyLanguage(text: String) {
        if (text.isBlank()) {
            _detectedLanguage.value = "Unknown"
            return
        }
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                _detectedLanguage.value = if (languageCode == "und") "Unknown" else languageCode
            }
            .addOnFailureListener {
                _detectedLanguage.value = "Unknown"
            }
    }

    private fun autoTranslate(text: String, targetLanguage: String) {
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }
        viewModelScope.launch {
            try {
                var sourceLang = TranslateLanguage.ENGLISH
                try {
                    val code = languageIdentifier.identifyLanguage(text).await()
                    if (code != "und" && code.length >= 2) {
                        sourceLang = code.substring(0, 2)
                    }
                } catch (e: Exception) {
                    Log.w("ScannerViewModel", "Could not identify language for auto-translate", e)
                }

                if (sourceLang == targetLanguage) {
                    _translatedText.value = text
                    return@launch
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLanguage)
                    .build()
                val translator = Translation.getClient(options)

                translator.downloadModelIfNeeded().await()
                val result = translator.translate(text).await()
                _translatedText.value = result
            } catch (e: Exception) {
                Log.w("ScannerViewModel", "Auto-translation in background failed: ${e.message}")
            }
        }
    }

    fun processPdfUri(context: Context, uri: Uri, onComplete: (() -> Unit)? = null) {
        processPdfFromUri(context, uri, onComplete)
    }

    fun processPdfFromUri(context: Context, uri: Uri, onComplete: (() -> Unit)? = null) {
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                if (fileDescriptor != null) {
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    val fullText = StringBuilder()
                    
                    val pageCount = minOf(pdfRenderer.pageCount, 5)
                    for (i in 0 until pageCount) {
                        val page = pdfRenderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val image = InputImage.fromBitmap(bitmap, 0)
                        try {
                            val visionText = recognizer.process(image).await()
                            fullText.append("--- Page ${i + 1} ---\n")
                            fullText.append(visionText.text).append("\n\n")
                        } catch (e: Exception) {
                            Log.e("ScannerViewModel", "Error scanning PDF page $i", e)
                        }
                    }
                    pdfRenderer.close()
                    fileDescriptor.close()

                    val resultText = fullText.toString().trim()
                    _currentExtractedText.value = if (resultText.isNotBlank()) resultText else "No text found in PDF."
                    identifyLanguage(_currentExtractedText.value)
                    autoTranslate(_currentExtractedText.value, _selectedTargetLanguage.value)
                }
                launch(Dispatchers.Main) { onComplete?.invoke() }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to process PDF", e)
                _currentExtractedText.value = "Failed to process PDF: ${e.message}"
                launch(Dispatchers.Main) { onComplete?.invoke() }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processMultipleImagesFromUris(context: Context, uris: List<Uri>, onComplete: (() -> Unit)? = null) {
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fullText = StringBuilder()
                for ((index, uri) in uris.withIndex()) {
                    try {
                        val image = InputImage.fromFilePath(context, uri)
                        val visionText = recognizer.process(image).await()
                        if (uris.size > 1) {
                            fullText.append("--- Image ${index + 1} ---\n")
                        }
                        fullText.append(visionText.text).append("\n\n")
                    } catch (e: Exception) {
                        Log.e("ScannerViewModel", "Failed to process image $uri", e)
                    }
                }
                val result = fullText.toString().trim()
                _currentExtractedText.value = if (result.isNotBlank()) result else "No text found in images."
                identifyLanguage(_currentExtractedText.value)
                autoTranslate(_currentExtractedText.value, _selectedTargetLanguage.value)
                launch(Dispatchers.Main) { onComplete?.invoke() }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed processing multiple images", e)
                _currentExtractedText.value = "Error processing images: ${e.message}"
                launch(Dispatchers.Main) { onComplete?.invoke() }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportToPdf(context: Context, text: String, title: String) {
        if (text.isBlank()) {
            _uiMessage.value = "No text to export as PDF."
            return
        }
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = PdfExportHelper.generateProfessionalDocumentPdf(
                    context = context,
                    title = title,
                    content = text,
                    detectedLang = _detectedLanguage.value
                )
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Professional PDF generated!"
                    PdfExportHelper.openOrSharePdf(context, file, title)
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to create PDF", e)
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Failed to generate PDF: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun generateSlidesFromText(text: String, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) {
            _uiMessage.value = "Document text is empty. Scan or paste text first."
            return
        }
        _isGeneratingSlides.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var generatedSlides: List<SlideItem>? = null
                val apiKey = BuildConfig.GEMINI_API_KEY

                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val prompt = """
                        You are an executive presentation creator. Convert the following document text into a structured, professional presentation slide deck (between 3 to 6 slides).
                        Return ONLY a valid JSON array of objects without backticks or markdown fences:
                        [
                          {
                            "title": "Concise Slide Title",
                            "subtitle": "Topic or Category Tag",
                            "bulletPoints": ["Point 1", "Point 2", "Point 3"],
                            "keyTakeaway": "One strong conclusion or takeaway"
                          }
                        ]
                        Document text:
                        $text
                        """.trimIndent()

                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            systemInstruction = Content(parts = listOf(Part(text = "You are a professional slide deck architect. Output pure valid JSON array only.")))
                        )
                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        val jsonString = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!jsonString.isNullOrBlank()) {
                            generatedSlides = parseSlidesFromJson(jsonString)
                        }
                    } catch (apiEx: Exception) {
                        Log.w("ScannerViewModel", "Gemini API slide generation failed, using local builder", apiEx)
                    }
                }

                if (generatedSlides.isNullOrEmpty()) {
                    generatedSlides = generateLocalFallbackSlides(text)
                }

                _slideDeck.value = generatedSlides
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Generated ${generatedSlides.size} presentation slides!"
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to generate slides", e)
                val fallback = generateLocalFallbackSlides(text)
                _slideDeck.value = fallback
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Generated ${fallback.size} presentation slides!"
                    onComplete?.invoke()
                }
            } finally {
                _isGeneratingSlides.value = false
            }
        }
    }

    private fun parseSlidesFromJson(rawJson: String): List<SlideItem> {
        val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
        val jsonArray = JSONArray(cleanJson)
        val result = mutableListOf<SlideItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val title = obj.optString("title", "Slide ${i + 1}")
            val subtitle = obj.optString("subtitle", "Key Insight")
            val bulletsArray = obj.optJSONArray("bulletPoints")
            val bullets = mutableListOf<String>()
            if (bulletsArray != null) {
                for (j in 0 until bulletsArray.length()) {
                    bullets.add(bulletsArray.getString(j))
                }
            }
            val takeaway = obj.optString("keyTakeaway", "")
            result.add(
                SlideItem(
                    slideNumber = i + 1,
                    title = title,
                    subtitle = subtitle,
                    bulletPoints = bullets,
                    keyTakeaway = takeaway
                )
            )
        }
        return result
    }

    private fun generateLocalFallbackSlides(text: String): List<SlideItem> {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val slides = mutableListOf<SlideItem>()

        val mainTitle = lines.firstOrNull()?.take(45) ?: "Document Executive Overview"
        val overviewBullets = lines.drop(1).take(3).map { it.take(110) }.ifEmpty {
            listOf("Extracted text summarized for presentation", "Organized into key points", "Ready for team review")
        }
        slides.add(
            SlideItem(
                slideNumber = 1,
                title = mainTitle,
                subtitle = "Executive Summary",
                bulletPoints = overviewBullets,
                keyTakeaway = "Primary document content synthesized"
            )
        )

        if (lines.size > 4) {
            val middleLines = lines.drop(4).take(4)
            slides.add(
                SlideItem(
                    slideNumber = 2,
                    title = "Key Findings & Analysis",
                    subtitle = "Core Content",
                    bulletPoints = middleLines.map { it.take(110) },
                    keyTakeaway = "Critical points highlighted from scan"
                )
            )
        }

        if (lines.size > 8) {
            val remainingLines = lines.drop(8).take(4)
            slides.add(
                SlideItem(
                    slideNumber = slides.size + 1,
                    title = "Action Items & Conclusion",
                    subtitle = "Next Steps",
                    bulletPoints = remainingLines.map { it.take(110) },
                    keyTakeaway = "Key takeaways for immediate implementation"
                )
            )
        } else if (slides.size < 2) {
            slides.add(
                SlideItem(
                    slideNumber = 2,
                    title = "Actionable Takeaways",
                    subtitle = "Highlights",
                    bulletPoints = listOf("Document successfully parsed and verified", "Exportable to professional PDF and slides"),
                    keyTakeaway = "Ready for presentation distribution"
                )
            )
        }

        return slides
    }

    fun exportSlidesToPdf(context: Context, title: String) {
        val slides = _slideDeck.value
        if (slides.isEmpty()) {
            _uiMessage.value = "No slides to export. Generate slides first."
            return
        }
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = PdfExportHelper.generateSlideDeckPdf(context, title, slides)
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Slide deck exported as PDF!"
                    PdfExportHelper.openOrSharePdf(context, file, "$title Presentation")
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to export slides PDF", e)
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Failed to export slide deck: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateExtractedText(text: String) {
        _currentExtractedText.value = text
    }

    fun translateText(
        text: String,
        targetLanguage: String = _selectedTargetLanguage.value,
        sourceLanguage: String = _selectedSourceLanguage.value
    ) {
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedSource = if (sourceLanguage == "auto" || sourceLanguage.isBlank()) {
                if (_detectedLanguage.value.length >= 2 && _detectedLanguage.value != "Unknown") {
                    _detectedLanguage.value.substring(0, 2)
                } else {
                    "en"
                }
            } else {
                sourceLanguage
            }

            val targetLangObj = LanguageCatalog.findByCode(targetLanguage)
            val targetName = targetLangObj?.name ?: targetLanguage
            val sourceLangObj = LanguageCatalog.findByCode(resolvedSource)
            val sourceName = sourceLangObj?.name ?: resolvedSource

            // Check if both are directly supported by ML Kit offline
            val mlKitSource = TranslateLanguage.fromLanguageTag(resolvedSource)
            val mlKitTarget = TranslateLanguage.fromLanguageTag(targetLanguage)

            if (mlKitSource != null && mlKitTarget != null) {
                try {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(mlKitSource)
                        .setTargetLanguage(mlKitTarget)
                        .build()
                    val translator = Translation.getClient(options)
                    translator.downloadModelIfNeeded().await()
                    val result = translator.translate(text).await()
                    _translatedText.value = result
                    _isProcessing.value = false
                    return@launch
                } catch (e: Exception) {
                    Log.w("ScannerViewModel", "ML Kit translation fallback to Gemini AI: ${e.message}")
                }
            }

            // High accuracy Gemini AI translation for all Indian & foreign languages
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    val prompt = """
                    You are a master professional translator. Translate the following text accurately from $sourceName to $targetName ($targetLanguage).
                    Maintain paragraphs, structure, punctuation, and native fluency.
                    Return ONLY the translated text without commentary, conversational prefixes, quotes, or markdown fences.

                    Text:
                    $text
                    """.trimIndent()

                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        systemInstruction = Content(parts = listOf(Part(text = "You are a professional linguistic translator. Output pure translation only.")))
                    )
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val candidateText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!candidateText.isNullOrBlank()) {
                        _translatedText.value = candidateText.trim()
                        _isProcessing.value = false
                        return@launch
                    }
                }
                _translatedText.value = "Translation for $targetName requires network connection."
            } catch (ex: Exception) {
                Log.e("ScannerViewModel", "AI Translation failed", ex)
                _translatedText.value = "Translation failed: ${ex.localizedMessage ?: "Network error"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportDocumentPdf(
        context: Context,
        text: String,
        title: String,
        onDone: ((File) -> Unit)? = null
    ) {
        if (text.isBlank()) {
            _uiMessage.value = "No text available to export as PDF."
            return
        }
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val safeTitle = if (title.isNotBlank()) title else "Document_${System.currentTimeMillis()}"
                val file = PdfExportHelper.generateProfessionalDocumentPdf(
                    context = context,
                    title = safeTitle,
                    content = text,
                    detectedLang = _detectedLanguage.value
                )
                _lastGeneratedPdf.value = file
                launch(Dispatchers.Main) {
                    _uiMessage.value = "PDF generated successfully!"
                    onDone?.invoke(file)
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to generate PDF", e)
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Failed to create PDF: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportTranslatedPdf(
        context: Context,
        title: String,
        translatedText: String,
        sourceText: String,
        sourceLang: String,
        targetLang: String,
        onDone: ((File) -> Unit)? = null
    ) {
        if (translatedText.isBlank()) {
            _uiMessage.value = "No translated text to export."
            return
        }
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val safeTitle = if (title.isNotBlank()) title else "Translated_${System.currentTimeMillis()}"
                val file = PdfExportHelper.generateTranslatedDocumentPdf(
                    context = context,
                    title = safeTitle,
                    translatedContent = translatedText,
                    sourceText = sourceText,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                )
                _lastGeneratedPdf.value = file
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Translated PDF created successfully!"
                    onDone?.invoke(file)
                }
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to create translated PDF", e)
                launch(Dispatchers.Main) {
                    _uiMessage.value = "Failed to export translated PDF: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun openLastGeneratedPdf(context: Context) {
        val file = _lastGeneratedPdf.value
        if (file != null && file.exists()) {
            PdfExportHelper.openPdf(context, file)
        } else {
            _uiMessage.value = "No generated PDF file found."
        }
    }

    fun shareLastGeneratedPdf(context: Context, title: String = "Document PDF") {
        val file = _lastGeneratedPdf.value
        if (file != null && file.exists()) {
            PdfExportHelper.sharePdf(context, file, title)
        } else {
            _uiMessage.value = "No generated PDF file found."
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
                _uiMessage.value = "Document saved to library!"
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            dao.deleteDocument(doc)
            _uiMessage.value = "Document removed."
        }
    }

    fun enhanceTextWithAI(originalText: String) {
        if (originalText.isBlank()) return
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    val prompt = "You are an AI assistant in a premium OCR application. Please enhance the following text extracted from an image. Fix spelling and grammar, format it clearly into readable sections with headings, and preserve the original meaning. Text:\n$originalText"
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        systemInstruction = Content(parts = listOf(Part(text = "You are a helpful OCR formatting assistant.")))
                    )
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val newText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!newText.isNullOrBlank()) {
                        _currentExtractedText.value = newText
                        _uiMessage.value = "Document enhanced with AI!"
                        return@launch
                    }
                }
                val localCleaned = cleanAndFormatTextLocally(originalText)
                _currentExtractedText.value = localCleaned
                _uiMessage.value = "Document formatted cleanly!"
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Failed to enhance text", e)
                val localCleaned = cleanAndFormatTextLocally(originalText)
                _currentExtractedText.value = localCleaned
                _uiMessage.value = "Document formatted with local cleaner!"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun cleanAndFormatTextLocally(text: String): String {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { line ->
                line.replace("\\s+".toRegex(), " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }
}
