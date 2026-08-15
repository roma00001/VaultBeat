package com.vaultbeat.core.utils

import java.util.Locale

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}

