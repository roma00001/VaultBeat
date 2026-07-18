package com.example.vaultbeat.core.network.model

sealed class DownloadProgress {
    data class Downloading(val percentage: Float, val speed: String) : DownloadProgress()
    data object Completed : DownloadProgress()
    data class Error(val message: String) : DownloadProgress()
}
