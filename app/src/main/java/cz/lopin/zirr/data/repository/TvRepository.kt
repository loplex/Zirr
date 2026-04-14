package cz.lopin.zirr.data.repository

import android.content.Context
import cz.lopin.zirr.data.local.RemoteDao
import cz.lopin.zirr.data.local.RemoteEntity
import cz.lopin.zirr.data.model.TvBrand
import cz.lopin.zirr.data.model.BrandRemotesData
import cz.lopin.zirr.data.model.RemoteVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source

class TvRepository(
    private val context: Context,
    private val remoteDao: RemoteDao
) {
    private val moshi = Moshi.Builder().build()

    suspend fun getTvBrands(): Result<List<TvBrand>> = withContext(Dispatchers.IO) {
        try {
            val list = context.assets.list("tv_brands_remotes") ?: emptyArray()
            val brands = list.map { fileName ->
                // Example format: "1016_Name.json" or "Brand.json". Handle removing extension and optional ID prefix
                val cleanedName = fileName.replace(".json", "")
                val nameWithoutId = if (cleanedName.contains("_")) cleanedName.substringAfter("_") else cleanedName
                TvBrand(nameWithoutId)
            }.sortedBy { it.name }
            Result.success(brands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSelectedRemote(): Flow<RemoteEntity?> = remoteDao.getSelectedRemote()

    suspend fun saveRemote(brandName: String, modelName: String? = null) {
        remoteDao.deselectAll()
        val remote = RemoteEntity(brandName = brandName, modelName = modelName, isSelected = true)
        remoteDao.insertRemote(remote)
    }

    suspend fun updateRemoteModel(id: Long, modelName: String) = withContext(Dispatchers.IO) {
        val entity = remoteDao.getRemoteById(id)
        if (entity != null) {
            remoteDao.updateRemote(entity.copy(modelName = modelName))
        }
    }

    suspend fun getRemoteVariants(brandName: String): List<RemoteVariant> = withContext(Dispatchers.IO) {
        try {
            val fileList = context.assets.list("tv_brands_remotes") ?: emptyArray()
            val targetFile = fileList.firstOrNull { it.contains(brandName, ignoreCase = true) }

            if (targetFile != null) {
                context.assets.open("tv_brands_remotes/$targetFile").source().buffer().use { source ->
                    val adapter = moshi.adapter(BrandRemotesData::class.java)
                    val data = adapter.fromJson(source)
                    return@withContext data?.remotes ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}
