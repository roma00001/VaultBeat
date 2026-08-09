package com.vaultbeat.core.utils

object MetadataUtils {
    
    /**
     * Cleans a track title by removing common noise patterns and extraneous info.
     */
    fun cleanTitle(title: String, artist: String): String {
        var cleaned = title
            .replace(Regex("\\(.*?\\)"), "") // Remove parentheses content
            .replace(Regex("\\[.*?]"), "")   // Remove brackets content
            
        // Remove common keywords
        val noiseRegex = Regex("(?i)\\b(official|video|audio|lyric|hd|4k|8k|hq|explicit|clean|remastered|live|envivo)\\b")
        cleaned = cleaned.replace(noiseRegex, "")
            
        // Remove artist name if it's at the start or end of the title
        val artistEscaped = Regex.escape(artist)
        cleaned = cleaned.replace(Regex("(?i)^$artistEscaped\\s*[-:–—|]\\s*"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*[-:–—|]\\s*$artistEscaped$"), "")
        
        // Remove leading/trailing symbols and spaces
        cleaned = cleaned.replace(Regex("^[:\\-–—|\\s.]+"), "")
        cleaned = cleaned.replace(Regex("[:\\-–—|\\s.\\d{4}]+$"), "")
            
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Cleans an artist name by removing common suffixes from YouTube uploaders.
     */
    fun cleanArtist(artist: String): String {
        return artist
            .replace("- Topic", "", ignoreCase = true)
            .replace(Regex("(?i)VEVO$"), "")
            .trim()
    }
}
