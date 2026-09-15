package com.example.data

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val isIndian: Boolean,
    val flag: String = if (isIndian) "🇮🇳" else "🌐"
)

object LanguageCatalog {
    val indianLanguages = listOf(
        LanguageItem("hi", "Hindi", "हिन्दी", isIndian = true, flag = "🇮🇳"),
        LanguageItem("bn", "Bengali", "বাংলা", isIndian = true, flag = "🇮🇳"),
        LanguageItem("te", "Telugu", "తెలుగు", isIndian = true, flag = "🇮🇳"),
        LanguageItem("mr", "Marathi", "मराठी", isIndian = true, flag = "🇮🇳"),
        LanguageItem("ta", "Tamil", "தமிழ்", isIndian = true, flag = "🇮🇳"),
        LanguageItem("ur", "Urdu", "اردو", isIndian = true, flag = "🇮🇳"),
        LanguageItem("gu", "Gujarati", "ગુજરાતી", isIndian = true, flag = "🇮🇳"),
        LanguageItem("kn", "Kannada", "ಕನ್ನಡ", isIndian = true, flag = "🇮🇳"),
        LanguageItem("ml", "Malayalam", "മലയാളം", isIndian = true, flag = "🇮🇳"),
        LanguageItem("pa", "Punjabi", "ਪੰਜਾਬੀ", isIndian = true, flag = "🇮🇳"),
        LanguageItem("or", "Odia", "ଓଡ଼ିଆ", isIndian = true, flag = "🇮🇳"),
        LanguageItem("as", "Assamese", "অসমীয়া", isIndian = true, flag = "🇮🇳"),
        LanguageItem("sa", "Sanskrit", "संस्कृतम्", isIndian = true, flag = "🇮🇳"),
        LanguageItem("ne", "Nepali", "नेपाली", isIndian = true, flag = "🇮🇳"),
        LanguageItem("kok", "Konkani", "कोंकणी", isIndian = true, flag = "🇮🇳"),
        LanguageItem("mai", "Maithili", "मैथिली", isIndian = true, flag = "🇮🇳"),
        LanguageItem("sd", "Sindhi", "سنڌي", isIndian = true, flag = "🇮🇳"),
        LanguageItem("ks", "Kashmiri", "کٲشُر", isIndian = true, flag = "🇮🇳")
    )

    val foreignLanguages = listOf(
        LanguageItem("en", "English", "English", isIndian = false, flag = "🇬🇧"),
        LanguageItem("es", "Spanish", "Español", isIndian = false, flag = "🇪🇸"),
        LanguageItem("fr", "French", "Français", isIndian = false, flag = "🇫🇷"),
        LanguageItem("de", "German", "Deutsch", isIndian = false, flag = "🇩🇪"),
        LanguageItem("it", "Italian", "Italiano", isIndian = false, flag = "🇮🇹"),
        LanguageItem("pt", "Portuguese", "Português", isIndian = false, flag = "🇵🇹"),
        LanguageItem("ru", "Russian", "Русский", isIndian = false, flag = "🇷🇺"),
        LanguageItem("ja", "Japanese", "日本語", isIndian = false, flag = "🇯🇵"),
        LanguageItem("ko", "Korean", "한국어", isIndian = false, flag = "🇰🇷"),
        LanguageItem("zh", "Chinese (Simplified)", "简体中文", isIndian = false, flag = "🇨🇳"),
        LanguageItem("zh-TW", "Chinese (Traditional)", "繁體中文", isIndian = false, flag = "🇹🇼"),
        LanguageItem("ar", "Arabic", "العربية", isIndian = false, flag = "🇸🇦"),
        LanguageItem("tr", "Turkish", "Türkçe", isIndian = false, flag = "🇹🇷"),
        LanguageItem("nl", "Dutch", "Nederlands", isIndian = false, flag = "🇳🇱"),
        LanguageItem("id", "Indonesian", "Bahasa Indonesia", isIndian = false, flag = "🇮🇩"),
        LanguageItem("th", "Thai", "ไทย", isIndian = false, flag = "🇹🇭"),
        LanguageItem("vi", "Vietnamese", "Tiếng Việt", isIndian = false, flag = "🇻🇳"),
        LanguageItem("fa", "Persian / Farsi", "فارسی", isIndian = false, flag = "🇮🇷"),
        LanguageItem("pl", "Polish", "Polski", isIndian = false, flag = "🇵🇱"),
        LanguageItem("sv", "Swedish", "Svenska", isIndian = false, flag = "🇸🇪"),
        LanguageItem("uk", "Ukrainian", "Українська", isIndian = false, flag = "🇺🇦"),
        LanguageItem("el", "Greek", "Ελληνικά", isIndian = false, flag = "🇬🇷"),
        LanguageItem("he", "Hebrew", "עברית", isIndian = false, flag = "🇮🇱"),
        LanguageItem("ms", "Malay", "Bahasa Melayu", isIndian = false, flag = "🇲🇾"),
        LanguageItem("tl", "Filipino / Tagalog", "Tagalog", isIndian = false, flag = "🇵🇭"),
        LanguageItem("ro", "Romanian", "Română", isIndian = false, flag = "🇷🇴"),
        LanguageItem("cs", "Czech", "Čeština", isIndian = false, flag = "🇨🇿"),
        LanguageItem("hu", "Hungarian", "Magyar", isIndian = false, flag = "🇭🇺"),
        LanguageItem("da", "Danish", "Dansk", isIndian = false, flag = "🇩🇰"),
        LanguageItem("fi", "Finnish", "Suomi", isIndian = false, flag = "🇫🇮"),
        LanguageItem("no", "Norwegian", "Norsk", isIndian = false, flag = "🇳🇴")
    )

    val allLanguages = indianLanguages + foreignLanguages

    fun findByCode(code: String): LanguageItem? {
        return allLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }

    fun getDisplayName(code: String): String {
        val lang = findByCode(code)
        return if (lang != null) "${lang.name} (${lang.nativeName})" else code.uppercase()
    }
}
