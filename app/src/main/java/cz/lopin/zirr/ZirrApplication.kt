package cz.lopin.zirr

import android.app.Application
import cz.lopin.zirr.di.AppModule

class ZirrApplication : Application() {
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
