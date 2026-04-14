package cz.lopin.zirr.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BrandRemotesData(
    val id: Int,
    @param:Json(name = "name_en") val nameEn: String,
    val remotes: List<RemoteVariant>
)

@JsonClass(generateAdapter = true)
data class RemoteVariant(
    val id: Int,
    @param:Json(name = "remote_number") val remoteNumber: String?,
    @param:Json(name = "remote_map") val remoteMap: String?,
    val remote: String?,
    val protocol: String?,
    val keys: Map<String, List<List<Int>>>? = emptyMap()
)
