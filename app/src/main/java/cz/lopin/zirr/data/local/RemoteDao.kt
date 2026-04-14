package cz.lopin.zirr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remotes")
    fun getAllRemotes(): Flow<List<RemoteEntity>>

    @Query("SELECT * FROM remotes WHERE isFavorite = 1")
    fun getFavoriteRemotes(): Flow<List<RemoteEntity>>

    @Query("SELECT * FROM remotes WHERE isSelected = 1 LIMIT 1")
    fun getSelectedRemote(): Flow<RemoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemote(remote: RemoteEntity)

    @Update
    suspend fun updateRemote(remote: RemoteEntity)

    @Query("UPDATE remotes SET isSelected = 0")
    suspend fun deselectAll()

    @Query("DELETE FROM remotes WHERE isSelected = 0 AND isFavorite = 0")
    suspend fun cleanupOrphanedSessions()

    @Query("DELETE FROM remotes WHERE brandName = :brandName AND modelName = :modelName AND isFavorite = 1")
    suspend fun deleteFavorite(brandName: String, modelName: String)

    @Transaction
    suspend fun setSelectedRemote(id: Long) {
        deselectAll()
        val remote = getRemoteById(id)
        remote?.let {
            updateRemote(it.copy(isSelected = true))
        }
    }

    @Query("SELECT * FROM remotes WHERE id = :id")
    suspend fun getRemoteById(id: Long): RemoteEntity?
}
