package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.PAYMENT_METHODS
import com.greenworldlighting.entrybook.core.blankToNull
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.newIdempotencyKey
import com.greenworldlighting.entrybook.core.normalizeRupees
import com.greenworldlighting.entrybook.core.paiseDisplay
import com.greenworldlighting.entrybook.core.paymentLabel
import com.greenworldlighting.entrybook.core.periodReady
import com.greenworldlighting.entrybook.core.subscriptionLabel
import com.greenworldlighting.entrybook.core.todayKolkata
import com.greenworldlighting.entrybook.core.typeLabel
import com.greenworldlighting.entrybook.data.ApiException
import com.greenworldlighting.entrybook.data.TransferRequest
import com.greenworldlighting.entrybook.ui.theme.inflowColor
import com.greenworldlighting.entrybook.ui.theme.outflowColor
import kotlinx.coroutines.launch

@Composable
private fun givenColor(): Color = if (isSystemInDarkTheme()) Color(0xFF8FCBB0) else Color(0xFF1B5E45)

@Composable
private fun receivedColor(): Color = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFF8D2F2F)

@Composable
private fun givenBackground(): Color = if (isSystemInDarkTheme()) Color(0xFF1B3A2E) else Color(0xFFD7E6DE)

@Composable
private fun receivedBackground(): Color = if (isSystemInDarkTheme()) Color(0xFF3A2222) else Color(0xFFF6E4E4)

@Composable
fun PersonalHomeScreen(onPerson: () -> Unit, onTransfer: () -> Unit, onExpense: () -> Unit) {
    val repo = LocalRepo.current
    val session = LocalSession.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.dashboard(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    BookScreen(title = session.account?.user?.name ?: "Personal", embedded = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            session.account?.subscription?.endDate?.let { Muted("Active until ${formatDay(it)}") }
            PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it })
            if (!ready) {
                Muted("Choose a start and end date.")
            } else {
                RemoteBody(remote) { dash ->
                    MoneyCard("Personal spend", dash.personal.spent.shown(), outflowColor(), "What you spent from your own pocket in this period.")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyCard("Given", dash.personal.given.shown(), givenColor(), modifier = Modifier.weight(1f), container = givenBackground())
                        MoneyCard("Received", dash.personal.received.shown(), receivedColor(), modifier = Modifier.weight(1f), container = receivedBackground())
                    }
                    PrimaryButton("Add friend or family", onClick = onPerson)
                    PrimaryButton("Record money", onClick = onTransfer)
                    PrimaryButton("Personal expense", onClick = onExpense)
                }
            }
        }
    }
}

@Composable
fun FriendDetailScreen(id: String, onBack: () -> Unit, onProfile: () -> Unit, onTransfer: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableIntStateOf(1) }
    var pageSize by rememberSaveable { mutableIntStateOf(20) }
    var settling by remember { mutableStateOf(false) }
    var settleError by remember { mutableStateOf<String?>(null) }
    var settleKey by remember { mutableStateOf(newIdempotencyKey()) }
    val party = rememberRemote(id) { repo.party("customer", id) }
    val ledger = rememberRemote("$id|$page|$pageSize") { repo.transfers(id, "this_month", null, null, page, pageSize) }
    var title by remember { mutableStateOf("Person") }
    BookScreen(title = title, onBack = onBack, embedded = true) { padding ->
        RemoteBody(party) { person ->
            SideEffect { title = person.name }
            FormColumn(padding) {
                RemoteBody(ledger) { book ->
                    val given = book.givenToDate?.paise ?: book.given.paise
                    val received = book.receivedToDate?.paise ?: book.received.paise
                    val net = given - received
                    val netColor = when {
                        net > 0 -> inflowColor()
                        net < 0 -> outflowColor()
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val netCaption = when {
                        net > 0 -> "owes you"
                        net < 0 -> "you owe"
                        else -> "settled"
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(netCaption, color = netColor, style = MaterialTheme.typography.labelLarge)
                            Text(
                                paiseDisplay(kotlin.math.abs(net)),
                                style = MaterialTheme.typography.headlineMedium,
                                color = netColor,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = onProfile) { Text("Profile") }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        settling = true
                                        settleError = null
                                        try {
                                            repo.settlePerson(id, settleKey)
                                            settleKey = newIdempotencyKey()
                                            ledger.reload()
                                        } catch (caught: Exception) {
                                            settleError = caught.message
                                        } finally {
                                            settling = false
                                        }
                                    }
                                },
                                enabled = !settling && net != 0L,
                            ) { Text(if (settling) "Settling…" else "Settle") }
                        }
                    }
                    ErrorLine(settleError)
                    val pages = ((book.total + pageSize - 1) / pageSize).coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("This month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onTransfer) { Icon(Icons.Filled.Add, contentDescription = "Record money") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyCard("Given", book.given.shown(), givenColor(), modifier = Modifier.weight(1f), container = givenBackground())
                        MoneyCard("Received", book.received.shown(), receivedColor(), modifier = Modifier.weight(1f), container = receivedBackground())
                    }
                    SectionLabel("Recent")
                    book.items.forEach { item ->
                        val settled = item.notes == "Settled"
                        val given = item.type == "personal_given"
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        settled -> MaterialTheme.colorScheme.surfaceVariant
                                        given -> givenBackground()
                                        else -> receivedBackground()
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "${formatDay(item.date)} · ${if (settled) "Settled" else typeLabel(item.type)} ${item.amount.shown()}",
                                color = if (settled) MaterialTheme.colorScheme.onSurface else if (given) givenColor() else receivedColor(),
                            )
                            Text(
                                "${paymentLabel(item.paymentMethod)} · ${item.status}",
                                color = if (given) givenColor() else receivedColor(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (book.items.isEmpty()) Muted("No money recorded with this person yet.")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { if (page > 1) page -= 1 }, enabled = page > 1) { Text("Previous") }
                        Text("$page / $pages")
                        TextButton(onClick = { if (page < pages) page += 1 }, enabled = page < pages) { Text("Next") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 50).forEach { size ->
                            FilterChip(selected = pageSize == size, onClick = { pageSize = size; page = 1 }, label = { Text("$size") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransferFormScreen(personId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val party = rememberRemote(personId) { repo.party("customer", personId) }
    var direction by rememberSaveable { mutableStateOf("given") }
    var date by rememberSaveable { mutableStateOf(todayKolkata()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("cash") }
    var notes by rememberSaveable { mutableStateOf("") }
    var confirmInactive by rememberSaveable { mutableStateOf(false) }
    var key by rememberSaveable { mutableStateOf(newIdempotencyKey()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    BookScreen(title = "Record money", onBack = onBack, embedded = true) { padding ->
        FormColumn(padding) {
            party.value?.let { Text(it.name, style = MaterialTheme.typography.titleLarge) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(direction == "given", { direction = "given" }, label = { Text("I gave") })
                FilterChip(direction == "received", { direction = "received" }, label = { Text("I received") })
            }
            DateField("Date", date) { date = it }
            AmountField("Amount", amount) { amount = it }
            ChoiceField("Payment method", paymentLabel(method), PAYMENT_METHODS) { method = it }
            NotesField(notes, { notes = it })
            if (party.value?.status == "inactive") {
                FilterChip(confirmInactive, { confirmInactive = !confirmInactive }, label = { Text("This person is inactive. Record anyway.") })
            }
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save", enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        repo.transfer(
                            personId,
                            TransferRequest(
                                direction = direction,
                                amount = normalizeRupees(amount),
                                date = date,
                                paymentMethod = method,
                                notes = notes.blankToNull(),
                                confirmInactive = confirmInactive,
                            ),
                            key,
                        )
                        onDone()
                    } catch (caught: Exception) {
                        error = caught.message
                        busy = false
                        if (caught is ApiException && caught.code != "NETWORK" && caught.statusCode != 409) key = newIdempotencyKey()
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalSpendScreen(onOpen: (String) -> Unit, onExpense: () -> Unit) {
    val repo = LocalRepo.current
    var page by rememberSaveable { mutableIntStateOf(1) }
    var pageSize by rememberSaveable { mutableIntStateOf(20) }
    val remote = rememberRemote("$page|$pageSize") {
        repo.transactions("this_month", null, null, "personal_expense", null, null, null, page, pageSize)
    }
    BookScreen(title = "Personal spend", embedded = true, actions = {
        IconButton(onClick = onExpense) { Icon(Icons.Filled.Add, contentDescription = "Add expense") }
    }) { padding ->
        RemoteBody(remote) { pageData ->
            val pages = ((pageData.total + pageSize - 1) / pageSize).coerceAtLeast(1)
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pageData.items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(item.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("${formatDay(item.date)} · ${item.amount.shown()}", style = MaterialTheme.typography.titleMedium)
                            Muted(item.notes ?: paymentLabel(item.paymentMethod))
                        }
                    }
                }
                if (pageData.items.isEmpty()) Muted("No personal expenses this month.")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (page > 1) page -= 1 }, enabled = page > 1) { Text("Previous") }
                    Text("$page / $pages")
                    TextButton(onClick = { if (page < pages) page += 1 }, enabled = page < pages) { Text("Next") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 50).forEach { size ->
                        FilterChip(selected = pageSize == size, onClick = { pageSize = size; page = 1 }, label = { Text("$size") })
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalMoreScreen(onNotifications: () -> Unit) {
    val session = LocalSession.current
    BookScreen(title = "More", embedded = true) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(session.account?.user?.name ?: "Personal", style = MaterialTheme.typography.titleLarge)
            Muted(session.account?.user?.email ?: "")
            session.account?.subscription?.let { StatusPill(subscriptionLabel(it.status), it.active) }
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNotifications),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) { Text("Notifications", modifier = Modifier.padding(16.dp)) }
            TextButton(onClick = { session.switchUsage("shop") }) { Text("Switch to shop owner") }
            Muted("Switching does not change the end date. An administrator can extend it.")
            TextButton(onClick = session.logout) { Text("Sign out") }
        }
    }
}
