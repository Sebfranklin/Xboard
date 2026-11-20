package com.example

import android.app.Application
import com.example.data.KeyCraftDatabase
import com.example.data.KeyCraftRepository
import com.example.data.KeyboardPreferences
import com.example.engine.SmartKeyboardEngine

class AppContainer(application: Application) {
    private val database: KeyCraftDatabase by lazy { KeyCraftDatabase.getInstance(application) }
    private val preferences: KeyboardPreferences by lazy { KeyboardPreferences(application) }
    val engine: SmartKeyboardEngine by lazy { SmartKeyboardEngine() }

    val repository: KeyCraftRepository by lazy {
        KeyCraftRepository(
            dao = database.keyCraftDao(),
            preferences = preferences,
            engine = engine
        ).also { it.preloadEngineMemory() }
    }
}

class KeyCraftApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
