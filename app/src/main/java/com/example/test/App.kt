package com.example.test

import android.app.Application
import com.example.test.di.IpfsHolder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var ipfsHolder: IpfsHolder

    override fun onCreate() {
        super.onCreate()
        ipfsHolder.warmUp()
    }
}
