import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Save the choice
    fun setDarkMode(context: Context, isDarkMode: Boolean) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(KEY_IS_DARK_MODE, isDarkMode)
        editor.apply()
        applyTheme(isDarkMode)
    }

    // Apply the theme to the app
    fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // Check what is currently saved
    fun isDarkMode(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_DARK_MODE, false)
    }
}