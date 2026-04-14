package com.thejakarnati.instanttelegram.data.repository

import com.thejakarnati.instanttelegram.data.local.FavoriteProfileDao
import com.thejakarnati.instanttelegram.data.local.FavoriteProfileEntity
import com.thejakarnati.instanttelegram.data.remote.InstagramApi
import com.thejakarnati.instanttelegram.domain.CreatorProfile
import com.thejakarnati.instanttelegram.domain.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreatorRepository(
    private val api: InstagramApi,
    private val favoritesDao: FavoriteProfileDao
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
}
