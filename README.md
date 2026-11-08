# AI Image To Text Scanner

An Android OCR and document productivity app. Photograph or import an image or a PDF, extract the text, translate it, chat about the document, and export the result as a PDF or a generated slide deck.

## Features

All of the following are implemented in this repository:

- Camera capture, single and multiple image import, and PDF input (`ScannerViewModel.processImage`, `processImageUri`, `processPdfFromUri`, `processMultipleImagesFromUris`)
- Text extraction through the Gemini API (`app/src/main/java/com/example/network/GeminiApiService.kt`)
- Automatic source-language detection and translation into a selectable target language (`translateText`, `LanguageCatalog`)
- Chat over the scanned document (`viewmodel/ChatViewModel.kt`, `ui/screens/ChatScreen.kt`)
- Slide-deck generation from the extracted text (`generateSlidesFromText`), with a local fallback when no API key is configured
- PDF export for documents, translations and slide decks (`util/PdfExportHelper.kt`)
- A local history of saved scans in Room (`data/AppDatabase.kt`, `DocumentDao`, `DocumentEntity`)
- Material 3 Compose UI with a selectable theme (`ui/theme/ThemeManager.kt`)

Screens: Splash, Home, Camera, Result, Translate, Chat, Slide Deck, About.

## Requirements

- Android Studio, or a machine with a JDK 21 and the Android SDK
- AGP 9.1.1, Kotlin 2.2.10, compileSdk 35, minSdk 24 (see `gradle/libs.versions.toml`)
- A Gemini API key for the AI-backed features

## Build

```bash
cp .env.example .env      # then set GEMINI_API_KEY
./gradlew assembleDebug
```

The key is injected at build time by the Secrets Gradle plugin as `BuildConfig.GEMINI_API_KEY`. Without a real key the app still builds and runs; the AI-backed paths fall back to local behaviour.

## Continuous integration

`.github/workflows/build.yml` builds a debug APK on every push and pull request, signs it with the standard Android debug keystore, uploads it as a workflow artifact, and attaches it to a GitHub Release.

## Tests

- Robolectric unit tests and a Roborazzi screenshot test in `app/src/test/`
- Instrumented tests in `app/src/androidTest/`

## Known limitations

- `patch.kt` in the repository root is a leftover code fragment and is not part of the Gradle build.
- Release builds require `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` plus an upload keystore. CI produces debug-signed APKs only.
- The application id is `com.aistudio.aiscanner.xrtpwz` and the Kotlin package is `com.example`; both come from the AI Studio project this app was generated from, and neither is a product name.

Created by **Umaiz Sufiyan**.
