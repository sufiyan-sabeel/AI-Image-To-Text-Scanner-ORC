package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String, val iconDesc: String) {
    SYSTEM("System Default", "Follows device dark/light setting"),
    LIGHT("Light Mode", "Always crisp and bright"),
    DARK("Dark Mode", "Deep OLED-friendly dark")
}

enum class AppColorPalette(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val lightContainer: Color,
    val darkContainer: Color
) {
    OCEAN_BLUE(
        displayName = "Ocean Blue",
        primaryColor = Color(0xFF0051BC),
        secondaryColor = Color(0xFF2B5BB5),
        lightContainer = Color(0xFFD8E2FF),
        darkContainer = Color(0xFF004094)
    ),
    EMERALD(
        displayName = "Emerald Mint",
        primaryColor = Color(0xFF0A6C44),
        secondaryColor = Color(0xFF1E825A),
        lightContainer = Color(0xFFB7F3D0),
        darkContainer = Color(0xFF005232)
    ),
    ROYAL_VIOLET(
        displayName = "Royal Violet",
        primaryColor = Color(0xFF6B46C1),
        secondaryColor = Color(0xFF805AD5),
        lightContainer = Color(0xFFE9D8FD),
        darkContainer = Color(0xFF553C9A)
    ),
    CRIMSON_ROSE(
        displayName = "Crimson Rose",
        primaryColor = Color(0xFFBE123C),
        secondaryColor = Color(0xFFE11D48),
        lightContainer = Color(0xFFFFE4E6),
        darkContainer = Color(0xFF881337)
    ),
    WARM_AMBER(
        displayName = "Warm Amber",
        primaryColor = Color(0xFFB45309),
        secondaryColor = Color(0xFFD97706),
        lightContainer = Color(0xFFFEF3C7),
        darkContainer = Color(0xFF78350F)
    ),
    CYBER_TEAL(
        displayName = "Cyber Teal",
        primaryColor = Color(0xFF0F766E),
        secondaryColor = Color(0xFF14B8A6),
        lightContainer = Color(0xFFCCFBF1),
        darkContainer = Color(0xFF115E59)
    );

    fun getLightColorScheme(): ColorScheme {
        return when (this) {
            OCEAN_BLUE -> lightColorScheme(
                primary = Color(0xFF0051BC),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD8E2FF),
                onPrimaryContainer = Color(0xFF001944),
                secondary = Color(0xFF2B5BB5),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFDCE1FF),
                onSecondaryContainer = Color(0xFF001848),
                tertiary = Color(0xFF006686),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFBEE9FF),
                onTertiaryContainer = Color(0xFF001F2B),
                background = Color(0xFFF7F9FB),
                onBackground = Color(0xFF191C1E),
                surface = Color(0xFFF7F9FB),
                onSurface = Color(0xFF191C1E),
                surfaceVariant = Color(0xFFE0E3E5),
                onSurfaceVariant = Color(0xFF424654),
                outline = Color(0xFF727786)
            )
            EMERALD -> lightColorScheme(
                primary = Color(0xFF0A6C44),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB7F3D0),
                onPrimaryContainer = Color(0xFF002111),
                secondary = Color(0xFF1E825A),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFC7F0DC),
                onSecondaryContainer = Color(0xFF002113),
                tertiary = Color(0xFF006877),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFA1EFFF),
                onTertiaryContainer = Color(0xFF001F25),
                background = Color(0xFFF6FBF7),
                onBackground = Color(0xFF181D1A),
                surface = Color(0xFFF6FBF7),
                onSurface = Color(0xFF181D1A),
                surfaceVariant = Color(0xFFDDE5DF),
                onSurfaceVariant = Color(0xFF404944),
                outline = Color(0xFF707974)
            )
            ROYAL_VIOLET -> lightColorScheme(
                primary = Color(0xFF6B46C1),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE9D8FD),
                onPrimaryContainer = Color(0xFF24005B),
                secondary = Color(0xFF7952B3),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFEDDCFF),
                onSecondaryContainer = Color(0xFF280A58),
                tertiary = Color(0xFF7D5260),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFD8E4),
                onTertiaryContainer = Color(0xFF31111D),
                background = Color(0xFFFCF7FF),
                onBackground = Color(0xFF1D1B20),
                surface = Color(0xFFFCF7FF),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFE7E0EB),
                onSurfaceVariant = Color(0xFF49454E),
                outline = Color(0xFF7A757F)
            )
            CRIMSON_ROSE -> lightColorScheme(
                primary = Color(0xFFBE123C),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD9DF),
                onPrimaryContainer = Color(0xFF40000F),
                secondary = Color(0xFF9E2A40),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFD9DF),
                onSecondaryContainer = Color(0xFF3B0716),
                tertiary = Color(0xFF805633),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFDCC2),
                onTertiaryContainer = Color(0xFF2F1500),
                background = Color(0xFFFFF8F7),
                onBackground = Color(0xFF23191A),
                surface = Color(0xFFFFF8F7),
                onSurface = Color(0xFF23191A),
                surfaceVariant = Color(0xFFF4DDDF),
                onSurfaceVariant = Color(0xFF534345),
                outline = Color(0xFF857375)
            )
            WARM_AMBER -> lightColorScheme(
                primary = Color(0xFF8A5100),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDCBE),
                onPrimaryContainer = Color(0xFF2D1600),
                secondary = Color(0xFF745B00),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFE087),
                onSecondaryContainer = Color(0xFF241A00),
                tertiary = Color(0xFF6B5E2F),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFF5E2A7),
                onTertiaryContainer = Color(0xFF231B00),
                background = Color(0xFFFFF8F4),
                onBackground = Color(0xFF211A14),
                surface = Color(0xFFFFF8F4),
                onSurface = Color(0xFF211A14),
                surfaceVariant = Color(0xFFF1DFD0),
                onSurfaceVariant = Color(0xFF50453B),
                outline = Color(0xFF827568)
            )
            CYBER_TEAL -> lightColorScheme(
                primary = Color(0xFF006A63),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF70F7EB),
                onPrimaryContainer = Color(0xFF00201D),
                secondary = Color(0xFF4A6360),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFCCE8E3),
                onSecondaryContainer = Color(0xFF05201D),
                tertiary = Color(0xFF456179),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFCCE5FF),
                onTertiaryContainer = Color(0xFF001E31),
                background = Color(0xFFF4FAF8),
                onBackground = Color(0xFF161D1C),
                surface = Color(0xFFF4FAF8),
                onSurface = Color(0xFF161D1C),
                surfaceVariant = Color(0xFFDAE5E2),
                onSurfaceVariant = Color(0xFF3F4947),
                outline = Color(0xFF6F7977)
            )
        }
    }

    fun getDarkColorScheme(): ColorScheme {
        return when (this) {
            OCEAN_BLUE -> darkColorScheme(
                primary = Color(0xFFAEC6FF),
                onPrimary = Color(0xFF002E6C),
                primaryContainer = Color(0xFF004397),
                onPrimaryContainer = Color(0xFFD8E2FF),
                secondary = Color(0xFFB5C4FF),
                onSecondary = Color(0xFF002B73),
                secondaryContainer = Color(0xFF0E429B),
                onSecondaryContainer = Color(0xFFDCE1FF),
                tertiary = Color(0xFF6AD3FF),
                onTertiary = Color(0xFF003547),
                tertiaryContainer = Color(0xFF004D65),
                onTertiaryContainer = Color(0xFFBEE9FF),
                background = Color(0xFF101418),
                onBackground = Color(0xFFE1E2E8),
                surface = Color(0xFF101418),
                onSurface = Color(0xFFE1E2E8),
                surfaceVariant = Color(0xFF424750),
                onSurfaceVariant = Color(0xFFC3C7D2),
                outline = Color(0xFF8D919C)
            )
            EMERALD -> darkColorScheme(
                primary = Color(0xFF70DAA2),
                onPrimary = Color(0xFF003920),
                primaryContainer = Color(0xFF005232),
                onPrimaryContainer = Color(0xFF8DF7BD),
                secondary = Color(0xFF86D6AC),
                onSecondary = Color(0xFF003822),
                secondaryContainer = Color(0xFF005233),
                onSecondaryContainer = Color(0xFFA2F3C7),
                tertiary = Color(0xFF84D2E6),
                onTertiary = Color(0xFF00363F),
                tertiaryContainer = Color(0xFF004E5A),
                onTertiaryContainer = Color(0xFFA1EFFF),
                background = Color(0xFF0F1511),
                onBackground = Color(0xFFE0E5DF),
                surface = Color(0xFF0F1511),
                onSurface = Color(0xFFE0E5DF),
                surfaceVariant = Color(0xFF404943),
                onSurfaceVariant = Color(0xFFC0C9C2),
                outline = Color(0xFF8A938D)
            )
            ROYAL_VIOLET -> darkColorScheme(
                primary = Color(0xFFCFBCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFE9D8FD),
                secondary = Color(0xFFCBC2DB),
                onSecondary = Color(0xFF332D41),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                tertiary = Color(0xFFEFB8C8),
                onTertiary = Color(0xFF4A2532),
                tertiaryContainer = Color(0xFF633B48),
                onTertiaryContainer = Color(0xFFFFD8E4),
                background = Color(0xFF141218),
                onBackground = Color(0xFFE6E1E6),
                surface = Color(0xFF141218),
                onSurface = Color(0xFFE6E1E6),
                surfaceVariant = Color(0xFF49454E),
                onSurfaceVariant = Color(0xFFCAC4CF),
                outline = Color(0xFF948F99)
            )
            CRIMSON_ROSE -> darkColorScheme(
                primary = Color(0xFFFFB2BC),
                onPrimary = Color(0xFF66001B),
                primaryContainer = Color(0xFF8F002A),
                onPrimaryContainer = Color(0xFFFFD9DF),
                secondary = Color(0xFFE5BDC1),
                onSecondary = Color(0xFF43292C),
                secondaryContainer = Color(0xFF5B3F42),
                onSecondaryContainer = Color(0xFFFFD9DF),
                tertiary = Color(0xFFE5C18D),
                onTertiary = Color(0xFF422C05),
                tertiaryContainer = Color(0xFF5B4219),
                onTertiaryContainer = Color(0xFFFFDCC2),
                background = Color(0xFF191113),
                onBackground = Color(0xFFEFE0E1),
                surface = Color(0xFF191113),
                onSurface = Color(0xFFEFE0E1),
                surfaceVariant = Color(0xFF524345),
                onSurfaceVariant = Color(0xFFD6C2C3),
                outline = Color(0xFF9F8C8E)
            )
            WARM_AMBER -> darkColorScheme(
                primary = Color(0xFFFFB77C),
                onPrimary = Color(0xFF4B2800),
                primaryContainer = Color(0xFF6A3B00),
                onPrimaryContainer = Color(0xFFFFDCBE),
                secondary = Color(0xFFE4C366),
                onSecondary = Color(0xFF3D2F00),
                secondaryContainer = Color(0xFF584500),
                onSecondaryContainer = Color(0xFFFFE087),
                tertiary = Color(0xFFD7C68D),
                onTertiary = Color(0xFF3B3005),
                tertiaryContainer = Color(0xFF52461A),
                onTertiaryContainer = Color(0xFFF5E2A7),
                background = Color(0xFF18120C),
                onBackground = Color(0xFFECE0DA),
                surface = Color(0xFF18120C),
                onSurface = Color(0xFFECE0DA),
                surfaceVariant = Color(0xFF50453B),
                onSurfaceVariant = Color(0xFFD4C4B5),
                outline = Color(0xFF9D8E81)
            )
            CYBER_TEAL -> darkColorScheme(
                primary = Color(0xFF4EDAD0),
                onPrimary = Color(0xFF003733),
                primaryContainer = Color(0xFF00504A),
                onPrimaryContainer = Color(0xFF70F7EB),
                secondary = Color(0xFFB0CCC7),
                onSecondary = Color(0xFF1B3532),
                secondaryContainer = Color(0xFF324B48),
                onSecondaryContainer = Color(0xFFCCE8E3),
                tertiary = Color(0xFFAECADF),
                onTertiary = Color(0xFF153349),
                tertiaryContainer = Color(0xFF2D4960),
                onTertiaryContainer = Color(0xFFCCE5FF),
                background = Color(0xFF0E1514),
                onBackground = Color(0xFFDEE4E2),
                surface = Color(0xFF0E1514),
                onSurface = Color(0xFFDEE4E2),
                surfaceVariant = Color(0xFF3F4947),
                onSurfaceVariant = Color(0xFFBEC9C6),
                outline = Color(0xFF899390)
            )
        }
    }
}

object ThemeManager {
    private const val PREFS_NAME = "app_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_PALETTE = "key_color_palette"
    private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _selectedPalette = MutableStateFlow(AppColorPalette.OCEAN_BLUE)
    val selectedPalette: StateFlow<AppColorPalette> = _selectedPalette.asStateFlow()

    private val _dynamicColor = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private var sharedPrefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPrefs != null) return
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedMode = sharedPrefs?.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        _themeMode.value = try {
            AppThemeMode.valueOf(savedMode)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }

        val savedPalette = sharedPrefs?.getString(KEY_PALETTE, AppColorPalette.OCEAN_BLUE.name) ?: AppColorPalette.OCEAN_BLUE.name
        _selectedPalette.value = try {
            AppColorPalette.valueOf(savedPalette)
        } catch (_: Exception) {
            AppColorPalette.OCEAN_BLUE
        }

        val defaultDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        _dynamicColor.value = sharedPrefs?.getBoolean(KEY_DYNAMIC_COLOR, defaultDynamic) ?: defaultDynamic
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        sharedPrefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun setPalette(palette: AppColorPalette) {
        _selectedPalette.value = palette
        // When picking an explicit palette, turn off dynamic color so the chosen palette takes effect
        _dynamicColor.value = false
        sharedPrefs?.edit()
            ?.putString(KEY_PALETTE, palette.name)
            ?.putBoolean(KEY_DYNAMIC_COLOR, false)
            ?.apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        sharedPrefs?.edit()?.putBoolean(KEY_DYNAMIC_COLOR, enabled)?.apply()
    }
}
