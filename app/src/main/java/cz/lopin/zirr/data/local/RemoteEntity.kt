package cz.lopin.zirr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remotes")
data class RemoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brandName: String,
    val modelName: String? = null,
    val isSelected: Boolean = false,
    val isFavorite: Boolean = false
)
