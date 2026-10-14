package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.periodReady
import com.greenworldlighting.entrybook.core.todayKolkata
import com.greenworldlighting.entrybook.ui.theme.inflowColor
import com.greenworldlighting.entrybook.ui.theme.outflowColor

@Composable
fun HomeScreen(onNotifications: () -> Unit, onSale: () -> Unit, onReceipt: () -> Unit, onPayment: () -> Unit, onExpense: () -> Unit, onReports: () -> Unit) {
    val repo = LocalRepo.current
    val session = LocalSession.current
    var showCash by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote(key = "$period|$from|$to", enabled = ready) {
        repo.dashboard(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    val title = session.account?.business?.name ?: "Entrybook"
    BookScreen(title = title, embedded = true, actions = {
        IconButton(onClick = onNotifications) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
        }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            session.account?.subscription?.endDate?.let { end ->
                Muted("Active until ${formatDay(end)}")
            }
            PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it })
            if (!ready) {
                Muted("Choose a start and end date.")
            } else {
                RemoteBody(remote) { dash ->
                    val inflow = inflowColor()
                    val outflow = outflowColor()
                    fun hidden(value: String) = if (showCash) value else "••••"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Cash", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showCash = !showCash }) {
                            Icon(
                                if (showCash) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showCash) "Hide cash" else "Show cash",
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MoneyCard("Cash in", hidden(dash.cash.inflow.shown()), inflow, modifier = Modifier.weight(1f))
                        MoneyCard("Cash out", hidden(dash.cash.outflow.shown()), outflow, modifier = Modifier.weight(1f))
                    }
                    MoneyCard("Net cash", hidden(dash.cash.net.shown()), if (dash.cash.net.paise >= 0) inflow else outflow, if (showCash) dash.cash.note else null)
                    MoneyCard("Invoiced sales", dash.sales.invoicedInPeriod.shown(), MaterialTheme.colorScheme.onSurface, dash.sales.note)
                    MoneyCard("Customer outstanding", dash.customers.outstanding.shown(), MaterialTheme.colorScheme.onSurface)
                    MoneyCard("Vendor payable", dash.vendors.payable.shown(), MaterialTheme.colorScheme.onSurface, dash.vendors.note)
                    MoneyCard("Personal expenses", dash.cash.personalExpense.shown(), outflow)
                    MoneyCard("Business outflow", dash.cash.businessOutflow.shown(), outflow, "Vendor payments plus business expenses.")
                    SectionLabel("Record")
                    PrimaryButton("New sale") { onSale() }
                    PrimaryButton("Receipt") { onReceipt() }
                    PrimaryButton("Vendor payment") { onPayment() }
                    PrimaryButton("Expense") { onExpense() }
                    TextButtonReports(onReports)
                }
            }
        }
    }
}

@Composable
private fun TextButtonReports(onReports: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onReports, modifier = Modifier.fillMaxWidth()) {
        Text("Open reports")
    }
}
