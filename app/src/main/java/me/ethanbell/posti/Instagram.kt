package me.ethanbell.posti

import android.net.Uri

object Instagram {
    /**
     * Check if a URL could reasonably correspond to a instagram post (image) by checking the URL and path
     */
    fun isInstaImage(uri: Uri): Boolean {
        return uri.host?.let { host: String ->
            setOf("instagr.am", "instagram.com")
                .any { host.contains(it, true) }
                .takeIf { _ ->
                    uri.pathSegments.any { "p".contentEquals(it) }
                }
        } ?: false
    }

    /**
     * Get the instagram identifier (shortcode) from an instagram image
     */
    fun shortCode(uri: Uri): String {
        val pathSegments: List<String> = uri.pathSegments
        return (pathSegments.indexOfFirst { "p".contentEquals(it) } + 1).let { pathSegments[it] }
    }

    /**
     * Get the high-quality (size=l) direct image URL for a given instagram post
     */
    fun directImageUri(shortcode: String) =
        Uri.parse("https://instagram.com/p/$shortcode/media/?size=l")
}