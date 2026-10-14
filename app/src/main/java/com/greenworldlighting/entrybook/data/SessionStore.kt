package com.greenworldlighting.entrybook.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.greenworldlighting.entrybook.core.DEFAULT_BASE_URL
import com.greenworldlighting.entrybook.core.normalizeBaseUrl
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "entrybook")

class SessionStore(context: Context) {
    private val dataStore = context.applicationContext.sessionDataStore

    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null
    @Volatile var baseUrl: String = DEFAULT_BASE_URL

    suspend fun hydrate() {
        val prefs = dataStore.data.first()
        accessToken = prefs[Keys.ACCESS]
        refreshToken = prefs[Keys.REFRESH]
        baseUrl = prefs[Keys.BASE] ?: DEFAULT_BASE_URL
    }

    suspend fun save(session: Session) {
        accessToken = session.accessToken
        refreshToken = session.refreshToken
        dataStore.edit {
            it[Keys.ACCESS] = session.accessToken
            it[Keys.REFRESH] = session.refreshToken
        }
    }

    suspend fun clear() {
        accessToken = null
        refreshToken = null
        dataStore.edit {
            it.remove(Keys.ACCESS)
            it.remove(Keys.REFRESH)
        }
    }

    suspend fun setBaseUrl(raw: String) {
        val normalized = normalizeBaseUrl(raw)
        baseUrl = normalized
        dataStore.edit { it[Keys.BASE] = normalized }
    }

    private object Keys {
        val ACCESS = stringPreferencesKey("access")
        val REFRESH = stringPreferencesKey("refresh")
        val BASE = stringPreferencesKey("base")
    }
}
