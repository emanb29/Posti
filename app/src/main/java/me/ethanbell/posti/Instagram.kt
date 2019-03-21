package me.ethanbell.posti

import android.net.Uri

object Instagram {
    fun isInstaLink(uri: Uri): Boolean {
        return uri.host?.let { host: String ->
            setOf("instagr.am", "instagram.com").any { host.contains(it, true) }
        } ?: false
    }
    fun shortCode(uri: Uri): String? {
        val pathSegments: List<String> = uri.pathSegments
        val codeIndex = pathSegments.indexOfFirst { it.contentEquals("p") } + 1
        return pathSegments.getOrNull(codeIndex)
    }
}