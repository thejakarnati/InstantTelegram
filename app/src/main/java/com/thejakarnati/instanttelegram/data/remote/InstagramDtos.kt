package com.thejakarnati.instanttelegram.data.remote

import com.squareup.moshi.Json

data class InstagramPageResponse(
    @Json(name = "graphql") val graphql: GraphqlDto?
)

data class InstagramWebProfileResponse(
    @Json(name = "data") val data: WebProfileDataDto?
)

data class WebProfileDataDto(
    @Json(name = "user") val user: UserDto?
)

data class GraphqlDto(
    @Json(name = "user") val user: UserDto?
)

data class UserDto(
    @Json(name = "username") val username: String?,
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "biography") val biography: String?,
    @Json(name = "profile_pic_url_hd") val profilePicUrlHd: String?,
    @Json(name = "edge_owner_to_timeline_media") val media: MediaConnectionDto?
)

data class MediaConnectionDto(
    @Json(name = "edges") val edges: List<MediaEdgeDto>?
)

data class MediaEdgeDto(
    @Json(name = "node") val node: MediaNodeDto?
)

data class MediaNodeDto(
    @Json(name = "id") val id: String?,
    @Json(name = "is_video") val isVideo: Boolean?,
    @Json(name = "display_url") val displayUrl: String?,
    @Json(name = "thumbnail_src") val thumbnailSrc: String?,
    @Json(name = "shortcode") val shortcode: String?,
    @Json(name = "edge_media_to_caption") val captionContainer: CaptionContainerDto?
)

data class CaptionContainerDto(
    @Json(name = "edges") val edges: List<CaptionEdgeDto>?
)

data class CaptionEdgeDto(
    @Json(name = "node") val node: CaptionNodeDto?
)

data class CaptionNodeDto(
    @Json(name = "text") val text: String?
)
