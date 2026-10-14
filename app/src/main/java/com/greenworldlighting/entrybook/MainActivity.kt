package com.greenworldlighting.entrybook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenworldlighting.entrybook.ui.AuthNav
import com.greenworldlighting.entrybook.ui.LocalRepo
import com.greenworldlighting.entrybook.ui.LocalSession
import com.greenworldlighting.entrybook.ui.LockedNav
import com.greenworldlighting.entrybook.ui.MainNav
import com.greenworldlighting.entrybook.ui.PersonalNav
import com.greenworldlighting.entrybook.ui.Phase
import com.greenworldlighting.entrybook.ui.SessionActions
import com.greenworldlighting.entrybook.ui.SessionViewModel
import com.greenworldlighting.entrybook.ui.SplashScreen
import com.greenworldlighting.entrybook.ui.theme.EntrybookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as EntrybookApp).container.repository
        setContent {
            val vm: SessionViewModel = viewModel(factory = SessionViewModel.factory(repo))
            val ui by vm.ui.collectAsState()
            EntrybookTheme {
                CompositionLocalProvider(
                    LocalRepo provides repo,
                    LocalSession provides SessionActions(ui.account, vm::logout, vm::refreshAccount, vm::switchUsage),
                ) {
                    when (ui.phase) {
                        Phase.Booting -> SplashScreen(ui.error, vm::boot)
                        Phase.SignedOut -> AuthNav(ui, vm)
                        Phase.Locked -> LockedNav(ui, vm)
                        Phase.Ready -> if (ui.account?.user?.usage == "personal") PersonalNav() else MainNav()
                    }
                }
            }
        }
    }
}
