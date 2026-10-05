package com.example.cutlist

import android.app.Application
import com.example.cutlist.data.AppContainer

class CutListApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
