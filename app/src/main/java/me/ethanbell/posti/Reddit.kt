package me.ethanbell.posti

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import net.dean.jraw.RedditClient
import net.dean.jraw.android.*
import net.dean.jraw.http.SimpleHttpLogger
import net.dean.jraw.models.EmbeddedMedia
import net.dean.jraw.oauth.AccountHelper
import java.util.*

object Reddit {
    private lateinit var accountHelper: AccountHelper
    private lateinit var tokenStore: SharedPreferencesTokenStore
    fun setup(ctx: Context): Unit {
        // Get UserAgent and OAuth2 data from AndroidManifest.xml
        val provider = ManifestAppInfoProvider(ctx.applicationContext);

        // Ideally, this should be unique to every device
        val deviceUuid = UUID.randomUUID()

        // Store our access tokens and refresh tokens in shared preferences
        tokenStore = SharedPreferencesTokenStore(ctx.applicationContext);
        // Load stored tokens into memory
        tokenStore.load();
        // Automatically save new tokens as they arrive
        tokenStore.autoPersist = true

        // An AccountHelper manages switching between accounts and into/out of userless mode.
        accountHelper = AndroidHelper.accountHelper(provider, deviceUuid, tokenStore);

        // Every time we use the AccountHelper to switch between accounts (from one account to
        // another, or into/out of userless mode), call this function
        accountHelper.onSwitch { redditClient: RedditClient ->
            // By default, JRAW logs HTTP activity to System.out. We're going to use Log.i()
            // instead.
            val logAdapter = SimpleAndroidLogAdapter(Log.INFO);

            // We're going to use the LogAdapter to write down the summaries produced by
            // SimpleHttpLogger
            redditClient.logger = SimpleHttpLogger(SimpleHttpLogger.DEFAULT_LINE_LENGTH, logAdapter)
        }
        GlobalScope.launch(Dispatchers.IO) {
            accountHelper.switchToUserless()
        }
    }

    fun maybeRedditImage(uri: Uri): Boolean {
        return (uri.host?.contains("reddit.com") ?: false
                ||
                uri.host?.contains("redd.it") ?: false) &&
                uri.pathSegments.any { "comments".contentEquals(it) }
    }

    fun getRedditImage(uri: Uri): Uri? {
        val s = runBlocking(Dispatchers.IO) {
            val a = getRedditImageAsync(uri)
            a
        }
        return s
    }

    suspend fun getRedditImageAsync(uri: Uri): Uri? {
        val id = (uri.pathSegments.indexOfFirst { "comments".contentEquals(it) } + 1).let { uri.pathSegments[it] }
        val post = GlobalScope.async {
            accountHelper.reddit.submission(id).inspect()
        }.await()
        return if (post.embeddedMedia != null) {
            post.embeddedMedia?.oEmbed?.let { obj: EmbeddedMedia.OEmbed ->
                when {
                    "photo".contentEquals(obj.type) -> obj.url
                    // this assumes oEmbed-adherent objects with strictly embeddable urls (read: not imgur albums with one image)
                    else /*eg "link", "video", "rich"*/ -> null
                }?.let { imgUri ->
                    kotlin.runCatching {
                        Uri.parse(imgUri)
                    }.getOrNull()
                }
            }
        } else if (post.isSelfPost) null // TODO("Reddit self post")
        else {
            kotlin.runCatching { Uri.parse(post.url) }.map { Util.getDirectImageUri(it) }.getOrNull()
        }
    }
}