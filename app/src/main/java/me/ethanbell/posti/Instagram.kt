package me.ethanbell.posti

import android.net.Uri

object Instagram {
    fun isInstaImage(uri: Uri): Boolean {
        return uri.host?.let { host: String ->
            setOf("instagr.am", "instagram.com")
                .any { host.contains(it, true) }
                .takeIf { _ ->
                    uri.pathSegments.any { "p".contentEquals(it) }
                }
        } ?: false
    }

    fun shortCode(uri: Uri): String {
        val pathSegments: List<String> = uri.pathSegments
        return (pathSegments.indexOfFirst { "p".contentEquals(it) } + 1).let { pathSegments[it] }
    }
}