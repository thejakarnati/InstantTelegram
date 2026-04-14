package com.thejakarnati.instanttelegram.data.repository

import com.thejakarnati.instanttelegram.data.local.FavoriteProfileDao
import com.thejakarnati.instanttelegram.data.local.FavoriteProfileEntity
import com.thejakarnati.instanttelegram.data.remote.BridgeApi
import com.thejakarnati.instanttelegram.data.remote.InstagramApi
import com.thejakarnati.instanttelegram.domain.CreatorProfile
import com.thejakarnati.instanttelegram.domain.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class CreatorRepository(
    private val bridgeApi: BridgeApi?,
    private val api: InstagramApi,
    private val favoritesDao: FavoriteProfileDao,
    private val httpClient: OkHttpClient
) {
    fun observeFavoriteProfiles(): Flow<List<CreatorProfile>> =
        favoritesDao.observeFavorites().map { entities ->
            entities.map {
                CreatorProfile(
                    username = it.username,
                    fullName = it.displayName,
                    biography = "",
                    profilePicUrl = it.profilePicUrl,
                    isFavorite = true
                )
            }
        }

    suspend fun searchProfile(username: String): Result<Pair<CreatorProfile, List<FeedItem>>> = runCatching {
        val normalized = username.trim()
        bridgeApi?.let { bridge ->
            runCatching { fetchFromBridge(bridge, normalized) }.getOrNull()?.let { return@runCatching it }
        }
        runCatching {
            val user = api.getProfilePageData(normalized).graphql?.user
                ?: api.getWebProfileInfo(normalized).data?.user
                ?: error("Profile unavailable.")
            val media = user.media?.edges.orEmpty().mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val postId = node.id ?: return@mapNotNull null
                val shortcode = node.shortcode ?: return@mapNotNull null
                FeedItem(
                    id = postId,
                    username = user.username.orEmpty(),
                    caption = node.captionContainer?.edges?.firstOrNull()?.node?.text.orEmpty(),
                    mediaUrl = node.displayUrl.orEmpty(),
                    thumbnailUrl = node.thumbnailSrc.orEmpty(),
                    isVideo = node.isVideo == true,
                    permalink = "https://www.instagram.com/p/$shortcode"
                )
            }

            val profile = CreatorProfile(
                username = user.username.orEmpty(),
                fullName = user.fullName.orEmpty(),
                biography = user.biography.orEmpty(),
                profilePicUrl = user.profilePicUrlHd.orEmpty(),
                isFavorite = false
            )

            profile to media
        }.getOrElse {
            fetchFromPublicHtml(normalized)
        }
    }

    suspend fun addFavoriteByUsername(username: String) {
        val normalized = username.trim()
        if (normalized.isBlank()) return
        favoritesDao.upsert(
            FavoriteProfileEntity(
                username = normalized,
                displayName = normalized,
                profilePicUrl = ""
            )
        )
    }

    suspend fun addFavorite(profile: CreatorProfile) {
        favoritesDao.upsert(
            FavoriteProfileEntity(
                username = profile.username,
                displayName = profile.fullName,
                profilePicUrl = profile.profilePicUrl
            )
        )
    }

    suspend fun removeFavorite(username: String) {
        favoritesDao.deleteByUsername(username)
    }

    private fun fetchFromPublicHtml(username: String): Pair<CreatorProfile, List<FeedItem>> {
        val request = Request.Builder()
            .url("https://www.instagram.com/$username/")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            .build()
        val response = httpClient.newCall(request).execute()
        val html = response.body?.string().orEmpty()
        if (!response.isSuccessful || html.isBlank()) error("Profile unavailable.")

        val doc = Jsoup.parse(html)
        val fullName = doc.select("meta[property=og:title]").attr("content")
            .substringBefore(" (@")
            .ifBlank { username }
        val bio = doc.select("meta[property=og:description]").attr("content")
            .substringAfter("Instagram: \"", "")
            .substringBefore("\"")
        val profilePic = doc.select("meta[property=og:image]").attr("content")

        val posts = doc.select("a[href^=/p/], a[href^=/reel/]")
            .take(24)
            .mapNotNull { anchor ->
                val href = anchor.attr("href")
                val shortcode = href.trim('/').split("/").lastOrNull().orEmpty()
                if (shortcode.isBlank()) return@mapNotNull null
                val imageUrl = anchor.select("img").firstOrNull()?.attr("src").orEmpty()
                val caption = anchor.select("img").firstOrNull()?.attr("alt").orEmpty()
                val isVideo = href.contains("/reel/")
                val typePrefix = if (isVideo) "reel" else "p"
                FeedItem(
                    id = shortcode,
                    username = username,
                    caption = caption,
                    mediaUrl = imageUrl,
                    thumbnailUrl = imageUrl,
                    isVideo = isVideo,
                    permalink = "https://www.instagram.com/$typePrefix/$shortcode/"
                )
            }
            .distinctBy { it.id }

        val profile = CreatorProfile(
            username = username,
            fullName = fullName,
            biography = bio,
            profilePicUrl = profilePic,
            isFavorite = false
        )

        return profile to posts
    }

    private suspend fun fetchFromBridge(bridge: BridgeApi, username: String): Pair<CreatorProfile, List<FeedItem>> {
        val response = bridge.getCreatorFeed(username)
        val profile = response.profile ?: error("Bridge profile missing.")
        val mappedProfile = CreatorProfile(
            username = profile.username ?: username,
            fullName = profile.fullName.orEmpty(),
            biography = profile.biography.orEmpty(),
            profilePicUrl = profile.profilePicUrl.orEmpty(),
            isFavorite = false
        )
        val mappedItems = response.items.orEmpty().mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            val permalink = item.permalink ?: return@mapNotNull null
            FeedItem(
                id = id,
                username = item.username ?: mappedProfile.username,
                caption = item.caption.orEmpty(),
                mediaUrl = item.mediaUrl.orEmpty(),
                thumbnailUrl = item.thumbnailUrl.orEmpty(),
                isVideo = item.isVideo == true,
                permalink = permalink
            )
        }
        return mappedProfile to mappedItems
    }
}
