package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.SlideItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun generateProfessionalDocumentPdf(
        context: Context,
        title: String,
        content: String,
        detectedLang: String
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - (2 * margin)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }

        // Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, pageWidth.toFloat(), 0f,
                Color.rgb(37, 99, 235), // Blue 600
                Color.rgb(14, 165, 233), // Sky 500
                Shader.TileMode.CLAMP
            )
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
        }

        val bulletAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
        }

        // Layout pages
        val pagesList = mutableListOf<PdfDocument.Page>()
        var currentPageNum = 1

        fun startNewPage(): Triple<PdfDocument.Page, Canvas, Float> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Draw top accent banner
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, headerAccentPaint)

            // Header (on first page full header, on subsequent pages compact header)
            var y = 40f
            if (currentPageNum == 1) {
                // First page header
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(title.take(45), margin, y, titlePaint)
                y += 20f

                // Meta row
                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 10f
                }
                val metaText = "$dateString  |  $wordCount words  |  Language: ${detectedLang.uppercase()}"
                canvas.drawText(metaText, margin, y, metaPaint)
                y += 16f

                // Divider
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 24f
            } else {
                // Running header
                val runningHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 9f
                }
                canvas.drawText(title.take(35), margin, y, runningHeaderPaint)
                val appLabel = "AI Scanner OCR"
                canvas.drawText(appLabel, pageWidth - margin - runningHeaderPaint.measureText(appLabel), y, runningHeaderPaint)
                y += 8f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 20f
            }

            return Triple(page, canvas, y)
        }

        var (currentPage, canvas, yOffset) = startNewPage()
        val maxY = pageHeight - 50f // Leave room for footer

        fun finishPageWithFooter(page: PdfDocument.Page, cv: Canvas, pageNum: Int) {
            // Footer line
            cv.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, dividerPaint)
            // Left footer
            cv.drawText("Generated by AI Scanner OCR • Confidential", margin, pageHeight - 22f, footerPaint)
            // Right footer
            val pageStr = "Page $pageNum"
            cv.drawText(pageStr, pageWidth - margin - footerPaint.measureText(pageStr), pageHeight - 22f, footerPaint)
            document.finishPage(page)
        }

        // Process lines and draw text
        val rawLines = content.split("\n")
        for (rawLine in rawLines) {
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                yOffset += 12f
                if (yOffset > maxY) {
                    finishPageWithFooter(currentPage, canvas, currentPageNum)
                    currentPageNum++
                    val newPageData = startNewPage()
                    currentPage = newPageData.first
                    canvas = newPageData.second
                    yOffset = newPageData.third
                }
                continue
            }

            val isHeading = line.startsWith("#") || line.startsWith("SECTION") || (line.length < 50 && line == line.uppercase() && line.any { it.isLetter() })
            val isBullet = line.startsWith("-") || line.startsWith("*") || line.startsWith("•") || line.matches("^\\d+\\..*".toRegex())

            val cleanLine = when {
                line.startsWith("###") -> line.removePrefix("###").trim()
                line.startsWith("##") -> line.removePrefix("##").trim()
                line.startsWith("#") -> line.removePrefix("#").trim()
                line.startsWith("-") || line.startsWith("*") || line.startsWith("•") -> line.substring(1).trim()
                else -> line
            }

            val activePaint = if (isHeading) headingPaint else textPaint
            val currentLineHeight = if (isHeading) 22f else 16f
            val leftIndent = if (isBullet) margin + 14f else margin
            val availableWidth = if (isBullet) contentWidth - 14f else contentWidth

            if (isHeading) {
                yOffset += 8f
            }

            // Word wrap
            var remaining = cleanLine
            var firstLineOfBullet = true

            while (remaining.isNotEmpty()) {
                if (yOffset > maxY) {
                    finishPageWithFooter(currentPage, canvas, currentPageNum)
                    currentPageNum++
                    val newPageData = startNewPage()
                    currentPage = newPageData.first
                    canvas = newPageData.second
                    yOffset = newPageData.third
                }

                val count = activePaint.breakText(remaining, true, availableWidth, null)
                val substring = remaining.substring(0, count)

                if (isBullet && firstLineOfBullet) {
                    // Draw stylish bullet dot
                    canvas.drawCircle(margin + 5f, yOffset - 4f, 2.5f, bulletAccentPaint)
                    firstLineOfBullet = false
                }

                canvas.drawText(substring, leftIndent, yOffset, activePaint)
                yOffset += currentLineHeight
                remaining = remaining.substring(count).trimStart()
            }

            if (isHeading) {
                yOffset += 6f
            }
        }

        finishPageWithFooter(currentPage, canvas, currentPageNum)

        // Save file
        val outputDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return file
    }

    fun generateSlideDeckPdf(
        context: Context,
        title: String,
        slides: List<SlideItem>
    ): File {
        val document = PdfDocument()
        // Standard 16:9 Presentation Landscape (842 x 595 pt)
        val pageWidth = 842
        val pageHeight = 595
        val margin = 50f
        val contentWidth = pageWidth - (2 * margin)

        val totalSlides = slides.size

        slides.forEachIndexed { index, slide ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background canvas: Clean modern off-white/light slate
            val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) } // Slate 50
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Presentation Frame / Card
            val cardPaint = Paint().apply { color = Color.WHITE }
            val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(20, 0, 0, 0)
            }
            val cardRect = RectF(margin - 10f, margin - 10f, pageWidth - margin + 10f, pageHeight - margin + 10f)
            canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)

            // Header Banner Strip
            val headerStripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, margin - 10f, pageWidth.toFloat(), margin - 10f,
                    Color.rgb(37, 99, 235), // Blue 600
                    Color.rgb(79, 70, 229), // Indigo 600
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(
                RectF(margin - 10f, margin - 10f, pageWidth - margin + 10f, margin + 4f),
                20f, 20f, headerStripPaint
            )

            var y = margin + 35f

            // Slide Category / Subtitle
            val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val catText = if (slide.subtitle.isNotBlank()) slide.subtitle.uppercase() else "AI PRESENTATION DECK"
            canvas.drawText(catText, margin + 15f, y, catPaint)

            // Slide Number Badge
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val badgeText = "SLIDE ${index + 1} OF $totalSlides"
            val badgeWidth = badgePaint.measureText(badgeText)
            canvas.drawText(badgeText, pageWidth - margin - 15f - badgeWidth, y, badgePaint)

            y += 28f

            // Slide Title
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(slide.title.take(50), margin + 15f, y, titlePaint)

            y += 18f
            // Divider
            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1.5f
            }
            canvas.drawLine(margin + 15f, y, pageWidth - margin - 15f, y, dividerPaint)

            y += 32f

            // Bullet Points
            val bulletTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val bulletDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
            }

            val maxBulletWidth = contentWidth - 60f

            slide.bulletPoints.forEach { point ->
                var remaining = point.trim()
                var isFirst = true

                while (remaining.isNotEmpty()) {
                    val count = bulletTextPaint.breakText(remaining, true, maxBulletWidth, null)
                    val sub = remaining.substring(0, count)

                    if (isFirst) {
                        canvas.drawCircle(margin + 25f, y - 5f, 4f, bulletDotPaint)
                        isFirst = false
                    }

                    canvas.drawText(sub, margin + 42f, y, bulletTextPaint)
                    y += 24f
                    remaining = remaining.substring(count).trimStart()
                }
                y += 8f
            }

            // Key Takeaway Pill / Card at bottom
            if (slide.keyTakeaway.isNotBlank()) {
                val takeawayY = pageHeight - margin - 75f
                val takeawayRect = RectF(margin + 15f, takeawayY, pageWidth - margin - 15f, takeawayY + 45f)
                val takeawayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(238, 242, 255) // Indigo 50
                }
                canvas.drawRoundRect(takeawayRect, 10f, 10f, takeawayBgPaint)

                val takeawayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(199, 210, 254) // Indigo 200
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(takeawayRect, 10f, 10f, takeawayBorderPaint)

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(79, 70, 229)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("TAKEAWAY: ", margin + 30f, takeawayY + 27f, labelPaint)

                val takeawayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 41, 59)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val labelWidth = labelPaint.measureText("TAKEAWAY: ")
                canvas.drawText(slide.keyTakeaway.take(80), margin + 30f + labelWidth, takeawayY + 27f, takeawayTextPaint)
            }

            // Slide Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9f
            }
            canvas.drawText("AI Scanner OCR • Presentation Slide Deck", margin + 15f, pageHeight - margin - 8f, footerPaint)

            document.finishPage(page)
        }

        val outputDir = File(context.cacheDir, "presentations").apply { mkdirs() }
        val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_Slides_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return file
    }

    fun generateTranslatedDocumentPdf(
        context: Context,
        title: String,
        translatedContent: String,
        sourceText: String,
        sourceLang: String,
        targetLang: String
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - (2 * margin)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, pageWidth.toFloat(), 0f,
                Color.rgb(16, 185, 129), // Emerald 500
                Color.rgb(59, 130, 246), // Blue 500
                Shader.TileMode.CLAMP
            )
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var currentPageNum = 1

        fun startNewPage(): Triple<PdfDocument.Page, Canvas, Float> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Top accent banner
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, headerAccentPaint)

            var y = 40f
            if (currentPageNum == 1) {
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(title.take(45), margin, y, titlePaint)
                y += 20f

                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 10f
                }
                val metaText = "$dateString  |  Translation: ${sourceLang.uppercase()} ➔ ${targetLang.uppercase()}"
                canvas.drawText(metaText, margin, y, metaPaint)
                y += 16f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 24f
            } else {
                val runningHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 9f
                }
                canvas.drawText("$title (Translated)", margin, y, runningHeaderPaint)
                val appLabel = "AI Scanner OCR"
                canvas.drawText(appLabel, pageWidth - margin - runningHeaderPaint.measureText(appLabel), y, runningHeaderPaint)
                y += 8f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 20f
            }

            return Triple(page, canvas, y)
        }

        var (currentPage, canvas, yOffset) = startNewPage()
        val maxY = pageHeight - 50f

        fun finishPageWithFooter(page: PdfDocument.Page, cv: Canvas, pageNum: Int) {
            cv.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, dividerPaint)
            cv.drawText("AI Scanner OCR • Translation Document", margin, pageHeight - 22f, footerPaint)
            val pageStr = "Page $pageNum"
            cv.drawText(pageStr, pageWidth - margin - footerPaint.measureText(pageStr), pageHeight - 22f, footerPaint)
            document.finishPage(page)
        }

        val rawLines = translatedContent.split("\n")
        for (rawLine in rawLines) {
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                yOffset += 12f
                if (yOffset > maxY) {
                    finishPageWithFooter(currentPage, canvas, currentPageNum)
                    currentPageNum++
                    val newPageData = startNewPage()
                    currentPage = newPageData.first
                    canvas = newPageData.second
                    yOffset = newPageData.third
                }
                continue
            }

            var remaining = line
            while (remaining.isNotEmpty()) {
                if (yOffset > maxY) {
                    finishPageWithFooter(currentPage, canvas, currentPageNum)
                    currentPageNum++
                    val newPageData = startNewPage()
                    currentPage = newPageData.first
                    canvas = newPageData.second
                    yOffset = newPageData.third
                }

                val count = textPaint.breakText(remaining, true, contentWidth, null)
                val substring = remaining.substring(0, count)
                canvas.drawText(substring, margin, yOffset, textPaint)
                yOffset += 16f
                remaining = remaining.substring(count).trimStart()
            }
        }

        finishPageWithFooter(currentPage, canvas, currentPageNum)

        val outputDir = File(context.cacheDir, "translations").apply { mkdirs() }
        val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_Translated_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return file
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to chooser/share if no dedicated viewer exists
            openOrSharePdf(context, file, file.nameWithoutExtension)
        }
    }

    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share PDF Document")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openOrSharePdf(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Open or Share PDF via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
