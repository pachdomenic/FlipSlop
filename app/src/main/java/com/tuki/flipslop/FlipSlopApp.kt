package com.tuki.flipslop

import android.app.Application
import android.content.Context
import com.tuki.flipslop.service.CoverRuntime

class FlipSlopApp : Application() {
    val runtime: CoverRuntime by lazy { CoverRuntime(this) }

    override fun onCreate() {
        super.onCreate()
        runtime.start()
    }
}

val Context.coverRuntime: CoverRuntime
    get() = (applicationContext as FlipSlopApp).runtime
