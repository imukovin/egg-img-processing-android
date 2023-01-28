package com.imukstudio.eggimgprocessing

import android.app.Application

class App: Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        const val APP_LOG_TAG = "EGG_IMG_PROCESSING"
    }
}
