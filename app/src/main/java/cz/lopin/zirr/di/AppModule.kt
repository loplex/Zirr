package cz.lopin.zirr.di

import android.content.Context
import androidx.room.Room
import cz.lopin.zirr.data.local.AppDatabase
import cz.lopin.zirr.data.repository.TvRepository
import cz.lopin.zirr.service.IrManager

class AppModule(private val context: Context) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "zirr_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val remoteDao = database.remoteDao()

    val tvRepository: TvRepository by lazy {
        TvRepository(context, remoteDao)
    }

    val irManager: IrManager by lazy {
        IrManager(context)
    }
}
