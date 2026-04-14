package cz.lopin.zirr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RemoteEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}
