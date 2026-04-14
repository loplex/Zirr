package cz.lopin.zirr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remotes WHERE isSelected = 1 LIMIT 1")
    fun getSelectedRemote(): Flow<RemoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemote(remote: RemoteEntity)

    @Update
    suspend fun updateRemote(remote: RemoteEntity)

    @Query("UPDATE remotes SET isSelected = 0")
    suspend fun deselectAll()

    @Query("SELECT * FROM remotes WHERE id = :id")
    suspend fun getRemoteById(id: Long): RemoteEntity?
}
