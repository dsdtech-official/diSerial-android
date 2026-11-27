package com.example.ui.i18n

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.example.platform.resolveSystemLanguageTag
import com.example.ui.model.AppLanguage
import java.util.Locale

fun resolveEffectiveLanguage(
    preference: AppLanguage,
    locale: Locale,
): AppLanguage = resolveEffectiveLanguage(preference, locale.toLanguageTag())

fun resolveAppStrings(
    preference: AppLanguage,
    locale: Locale,
): AppStrings = resolveAppStrings(preference, locale.toLanguageTag())

fun resolveContextLocale(context: Context): Locale {
    return ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
}

fun resolveContextAppStrings(context: Context, preference: AppLanguage): AppStrings =
    resolveAppStrings(preference, resolveSystemLanguageTag(context))
