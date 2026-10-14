package com.greenworldlighting.entrybook

import android.app.Application
import com.greenworldlighting.entrybook.data.ApiClient
import com.greenworldlighting.entrybook.data.EntrybookRepository
import com.greenworldlighting.entrybook.data.SessionStore
import kotlinx.coroutines.runBlocking

class EntrybookApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runBlocking { container.store.hydrate() }
    }
}

class AppContainer(context: Application) {
    val store = SessionStore(context)
    val api = ApiClient(store)
    val repository = EntrybookRepository(api, store)
}
