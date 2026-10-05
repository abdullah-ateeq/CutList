package com.example.cutlist.data

import com.example.cutlist.domain.LengthUnit

enum class ThemeMode(val label: String) { LIGHT("Light"), DARK("Dark"), SYSTEM("System") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultUnit: LengthUnit = LengthUnit.MM,
    val decimalPlaces: Int = 2,
    val consolidateIdentical: Boolean = true,
    val showCutOrder: Boolean = true,
    val showAreaSummary: Boolean = true,
    val showMaterialSummary: Boolean = true,
    val showThicknessSummary: Boolean = true,
    val hapticFeedback: Boolean = true,
    val confirmDelete: Boolean = true,
)
