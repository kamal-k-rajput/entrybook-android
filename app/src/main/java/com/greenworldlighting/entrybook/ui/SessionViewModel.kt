package com.greenworldlighting.entrybook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenworldlighting.entrybook.core.emailError
import com.greenworldlighting.entrybook.core.passwordError
import com.greenworldlighting.entrybook.data.ApiException
import com.greenworldlighting.entrybook.data.BusinessProfile
import com.greenworldlighting.entrybook.data.EntrybookRepository
import com.greenworldlighting.entrybook.data.Me
import com.greenworldlighting.entrybook.data.Subscription
import com.greenworldlighting.entrybook.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Account(
    val user: UserProfile,
    val business: BusinessProfile?,
    val subscription: Subscription?,
)

enum class Phase { Booting, SignedOut, Locked, Ready }

data class SessionUi(
    val phase: Phase = Phase.Booting,
    val account: Account? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class SessionViewModel(private val repo: EntrybookRepository) : ViewModel() {
    private val _ui = MutableStateFlow(SessionUi())
    val ui: StateFlow<SessionUi> = _ui.asStateFlow()

    init {
        repo.onUnauthorized = {
            viewModelScope.launch {
                repo.store.clear()
                _ui.value = SessionUi(phase = Phase.SignedOut, error = "Your session ended. Sign in again.")
            }
        }
        repo.onSubscriptionRequired = { subscription ->
            viewModelScope.launch { lock(subscription) }
        }
        boot()
    }

    fun boot() {
        viewModelScope.launch {
            _ui.value = SessionUi(phase = Phase.Booting)
            if (repo.store.refreshToken == null) {
                _ui.value = SessionUi(phase = Phase.SignedOut)
                return@launch
            }
            try {
                apply(loadMe())
            } catch (error: ApiException) {
                if (error.statusCode != 401) {
                    _ui.value = SessionUi(phase = Phase.Booting, error = error.message)
                    return@launch
                }
                try {
                    repo.refresh()
                    apply(loadMe())
                } catch (_: Exception) {
                    repo.store.clear()
                    _ui.value = SessionUi(phase = Phase.SignedOut, error = "Your session ended. Sign in again.")
                }
            } catch (error: Exception) {
                _ui.value = SessionUi(phase = Phase.Booting, error = error.message ?: "Couldn't reach Entrybook.")
            }
        }
    }

    fun login(email: String, password: String, server: String) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                emailError(email)?.let { throw IllegalArgumentException(it) }
                passwordError(password)?.let { throw IllegalArgumentException(it) }
                repo.setBaseUrl(server)
                repo.login(email.trim(), password)
                apply(loadMe())
            } catch (error: Exception) {
                _ui.value = SessionUi(phase = Phase.SignedOut, busy = false, error = error.message ?: "Sign-in failed")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        businessName: String,
        usage: String,
        server: String,
    ) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                if (name.trim().length < 2) throw IllegalArgumentException("Enter your name")
                if (usage == "shop" && businessName.trim().length < 2) throw IllegalArgumentException("Enter the shop name")
                emailError(email)?.let { throw IllegalArgumentException(it) }
                passwordError(password)?.let { throw IllegalArgumentException(it) }
                repo.setBaseUrl(server)
                repo.register(name.trim(), email.trim(), phone, password, businessName.trim(), usage)
                apply(loadMe())
            } catch (error: Exception) {
                _ui.update { it.copy(busy = false, error = error.message ?: "Couldn't create the account") }
            }
        }
    }

    fun switchUsage(usage: String) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                apply(repo.setUsage(usage))
            } catch (error: Exception) {
                _ui.update { it.copy(busy = false, error = error.message) }
            }
        }
    }

    fun refreshAccount() {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                apply(loadMe())
            } catch (error: Exception) {
                _ui.update { it.copy(busy = false, error = error.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _ui.value = SessionUi(phase = Phase.SignedOut)
        }
    }

    private suspend fun loadMe(): Me = repo.me(notifyAuth = false)

    private suspend fun apply(me: Me) {
        if (me.user.role != "merchant") {
            repo.logout()
            _ui.value = SessionUi(
                phase = Phase.SignedOut,
                error = "This app is for shop owners. Administrators use the website.",
            )
            return
        }
        val account = Account(me.user, me.business, me.subscription)
        _ui.value = SessionUi(
            phase = if (account.subscription?.active == true) Phase.Ready else Phase.Locked,
            account = account,
        )
    }

    private fun lock(subscription: Subscription?) {
        val current = _ui.value.account ?: return
        val next = subscription ?: current.subscription?.copy(active = false)
        _ui.value = _ui.value.copy(
            phase = Phase.Locked,
            account = current.copy(subscription = next),
            busy = false,
        )
    }

    companion object {
        fun factory(repo: EntrybookRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(repo) as T
            }
    }
}
