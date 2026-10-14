package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.blankToNull
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.newIdempotencyKey
import com.greenworldlighting.entrybook.core.formatWhen
import com.greenworldlighting.entrybook.core.collectLabel
import com.greenworldlighting.entrybook.core.paiseDisplay
import com.greenworldlighting.entrybook.core.normalizeRupees
import com.greenworldlighting.entrybook.core.paymentLabel
import com.greenworldlighting.entrybook.core.typeLabel
import com.greenworldlighting.entrybook.data.Party
import com.greenworldlighting.entrybook.data.PartyRequest
import com.greenworldlighting.entrybook.ui.theme.inflowColor
import com.greenworldlighting.entrybook.ui.theme.outflowColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PartyListScreen(
    kind: String,
    purpose: String,
    onBack: (() -> Unit)?,
    onCreate: () -> Unit,
    onOpen: (Party) -> Unit,
    onDetails: (Party) -> Unit,
    listTitle: String? = null,
    showNet: Boolean = false,
    showBalance: Boolean = false,
    showPaid: Boolean = false,
) {
    val repo = LocalRepo.current
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<Party>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var summaryGiven by remember { mutableStateOf(0L) }
    var summaryReceived by remember { mutableStateOf(0L) }
    var summaryPaid by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) {
        delay(300)
        debounced = query
    }
    LaunchedEffect(debounced, status) { page = 1 }
    LaunchedEffect(debounced, status, page) {
        loading = true
        error = null
        try {
            val result = repo.parties(
                kind,
                debounced,
                status.blankToNull(),
                page,
                when {
                    showNet -> "personal"
                    showBalance -> "outstanding"
                    showPaid -> "paid"
                    else -> null
                },
            )
            items = if (page == 1) result.items else items + result.items
            total = result.total
            if (page == 1) {
                summaryGiven = result.given?.paise ?: result.items.sumOf { it.given?.paise ?: 0L }
                summaryReceived = result.received?.paise ?: result.items.sumOf { it.received?.paise ?: 0L }
                summaryPaid = result.paid?.paise ?: result.items.sumOf { it.paid?.paise ?: 0L }
            }
        } catch (caught: Exception) {
            error = caught.message
        } finally {
            loading = false
        }
    }
    val noun = if (kind == "vendor") "vendor" else "customer"
    val title = when (purpose) {
        "sale" -> "Choose a customer"
        "receipt" -> "Choose a customer"
        "pay" -> "Choose a vendor"
        else -> listTitle ?: if (kind == "vendor") "Vendors" else "Customers"
    }
    val ledgerList = showNet || showPaid
    BookScreen(title = title, onBack = onBack, embedded = true, actions = {
        if (ledgerList) {
            IconButton(onClick = { searching = !searching }) { Icon(Icons.Filled.Search, contentDescription = "Search") }
        }
        IconButton(onClick = onCreate) { Icon(Icons.Filled.Add, contentDescription = "Add $noun") }
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (ledgerList) 0.dp else 10.dp),
        ) {
            if (ledgerList && searching) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        label = { Text("Search name") },
                    )
                }
            }
            if (!ledgerList) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search name, phone, or code") },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to "All", "active" to "Active", "inactive" to "Inactive").forEach { (id, label) ->
                            FilterChip(selected = status == id, onClick = { status = id }, label = { Text(label) })
                        }
                    }
                }
            }
            if (ledgerList && !loading && error == null) {
                item {
                    LedgerSummary(showNet, summaryGiven, summaryReceived, summaryPaid)
                }
            }
            if (error != null && items.isEmpty()) {
                item { ErrorLine(error); TextButton(onClick = { page = page }) { Text("Try again") } }
            }
            if (!loading && items.isEmpty() && error == null) {
                item { Muted(if (showNet) "No friends yet." else "No ${noun}s yet.") }
            }
            items(items, key = { it.id }) { party ->
                if (ledgerList) {
                    LedgerPersonRow(party, showPaid = showPaid, onClick = { onOpen(party) })
                } else {
                    PartyRow(party, showDetails = purpose.isNotBlank(), showNet = false, showBalance = showBalance, onClick = { onOpen(party) }, onDetails = { onDetails(party) })
                }
            }
            if (loading) item { LoadingBox() }
            if (!loading && items.size < total) {
                item { TextButton(onClick = { page += 1 }, modifier = Modifier.fillMaxWidth()) { Text("Load more") } }
            }
        }
    }
}

@Composable
private fun LedgerSummary(friends: Boolean, given: Long, received: Long, paid: Long) {
    val net = given - received
    val color = when {
        !friends -> inflowColor()
        net > 0 -> inflowColor()
        net < 0 -> outflowColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        !friends -> "Total paid to vendors"
        net > 0 -> "Overall, you are owed"
        net < 0 -> "Overall, you owe"
        else -> "Overall, settled"
    }
    val amount = if (friends) paiseDisplay(kotlin.math.abs(net)) else paiseDisplay(paid)
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(amount, color = color, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun LedgerPersonRow(party: Party, showPaid: Boolean, onClick: () -> Unit) {
    val given = party.given?.paise ?: 0L
    val received = party.received?.paise ?: 0L
    val paid = party.paid?.paise ?: 0L
    val net = given - received
    val color = when {
        showPaid -> if (paid == 0L) MaterialTheme.colorScheme.onSurfaceVariant else inflowColor()
        net > 0 -> inflowColor()
        net < 0 -> outflowColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val caption = when {
        showPaid && paid == 0L -> "no payments"
        showPaid -> "paid"
        net > 0 -> "owes you"
        net < 0 -> "you owe"
        given == 0L && received == 0L -> "no expenses"
        else -> "settled"
    }
    val amount = when {
        showPaid && paid == 0L -> null
        showPaid -> paiseDisplay(paid)
        net == 0L -> null
        else -> paiseDisplay(kotlin.math.abs(net))
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialAvatar(party.name)
        Spacer(Modifier.size(12.dp))
        Text(
            party.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(caption, color = color, style = MaterialTheme.typography.labelMedium)
            if (amount != null) {
                Text(amount, color = color, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun InitialAvatar(name: String) {
    val colors = listOf(
        Color(0xFF1B5E45),
        Color(0xFF3D6B8C),
        Color(0xFF8D2F2F),
        Color(0xFFA67C32),
        Color(0xFF5C4B8A),
        Color(0xFF2F6B5A),
    )
    val color = colors[kotlin.math.abs(name.hashCode()).mod(colors.size)]
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(letter, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PartyRow(party: Party, showDetails: Boolean, showNet: Boolean, showBalance: Boolean, onClick: () -> Unit, onDetails: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(party.name, style = MaterialTheme.typography.titleMedium)
                if (showBalance) {
                    val due = party.outstanding?.paise ?: 0L
                    val color = when {
                        due > 0 -> outflowColor()
                        due < 0 -> inflowColor()
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(collectLabel(due), color = color, style = MaterialTheme.typography.titleMedium)
                } else if (showNet) {
                    val given = party.given?.paise ?: 0L
                    val received = party.received?.paise ?: 0L
                    val color = when {
                        given > received -> outflowColor()
                        received > given -> inflowColor()
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(paiseDisplay(kotlin.math.abs(given - received)), color = color, style = MaterialTheme.typography.titleMedium)
                } else {
                    val meta = listOfNotNull(party.phone, party.code, party.businessName).joinToString(" · ")
                    if (meta.isNotBlank()) Muted(meta)
                }
                if (party.openingBalance.paise != 0L) Muted("Opening ${party.openingBalance.shown()}")
                if (party.status != "active") StatusPill("Inactive", active = false)
            }
            if (showDetails) {
                IconButton(onClick = onDetails) { Icon(Icons.Filled.Info, contentDescription = "Details") }
            }
        }
    }
}

@Composable
fun PartyFormScreen(kind: String, partyId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var business by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var opening by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(partyId == null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(partyId) {
        if (partyId != null && !loaded) {
            try {
                val party = repo.party(kind, partyId)
                name = party.name
                phone = party.phone.orEmpty()
                business = party.businessName.orEmpty()
                address = party.address.orEmpty()
                code = party.code.orEmpty()
                notes = party.notes.orEmpty()
                opening = party.openingBalance.rupees
                loaded = true
            } catch (caught: Exception) {
                error = caught.message
            }
        }
    }
    val title = if (partyId == null) "New ${if (kind == "vendor") "vendor" else "customer"}" else "Edit"
    BookScreen(title = title, onBack = onBack, embedded = true) { padding ->
        if (!loaded && error == null) {
            LoadingBox()
            return@BookScreen
        }
        FormColumn(padding) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(business, { business = it }, label = { Text("Business name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(code, { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            NotesField(notes, { notes = it })
            AmountField(if (kind == "vendor") "Opening payable" else "Opening receivable", opening, { opening = it })
            Muted("Zero is allowed. This is not a bill or a sale.")
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save", enabled = !busy && loaded) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        if (name.trim().length < 2) throw IllegalArgumentException("Enter a name")
                        val balance = opening.blankToNull()?.let { normalizeRupees(it, allowZero = true) }
                        val saved = repo.saveParty(
                            kind,
                            partyId,
                            PartyRequest(
                                name = name.trim(),
                                phone = phone.blankToNull(),
                                businessName = business.blankToNull(),
                                address = address.blankToNull(),
                                code = code.blankToNull(),
                                notes = notes.blankToNull(),
                                openingBalance = balance,
                            ),
                        )
                        onSaved(saved.id)
                    } catch (caught: Exception) {
                        error = caught.message
                        busy = false
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(
    id: String,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onSale: () -> Unit,
    onReceive: () -> Unit,
    onView: (String, String) -> Unit,
) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableIntStateOf(1) }
    var pageSize by rememberSaveable { mutableIntStateOf(20) }
    var settling by remember { mutableStateOf(false) }
    var settleError by remember { mutableStateOf<String?>(null) }
    var settleKey by remember { mutableStateOf(newIdempotencyKey()) }
    val remote = rememberRemote("$id|$page|$pageSize") {
        CustomerSnapshot(repo.party("customer", id), repo.customerLedger(id, "this_month", null, null, page, pageSize))
    }
    BookScreen(title = "Customer", onBack = onBack, embedded = true) { padding ->
        RemoteBody(remote) { snap ->
            val pages = ((snap.ledger.total + pageSize - 1) / pageSize).coerceAtLeast(1)
            FormColumn(padding) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(snap.party.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onProfile) { Text("Profile") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val due = snap.ledger.outstanding.paise
                    val dueColor = when {
                            due > 0 -> outflowColor()
                            due < 0 -> inflowColor()
                            else -> MaterialTheme.colorScheme.onSurface
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (due < 0) "Advance" else "Collect", style = MaterialTheme.typography.labelLarge, color = dueColor)
                        Text(paiseDisplay(kotlin.math.abs(due)), style = MaterialTheme.typography.headlineMedium, color = dueColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = onSale) { Icon(Icons.Filled.Add, contentDescription = "Record sale") }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    settling = true
                                    settleError = null
                                    try {
                                        repo.settleCustomer(id, settleKey)
                                        settleKey = newIdempotencyKey()
                                        remote.reload()
                                    } catch (caught: Exception) {
                                        settleError = caught.message
                                    } finally {
                                        settling = false
                                    }
                                }
                            },
                            enabled = !settling && due != 0L,
                        ) { Text(if (settling) "Settling…" else "Settle") }
                    }
                }
                ErrorLine(settleError)
                TextButton(onClick = onReceive) { Text("Receive udhar") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyCard("Sold this month", snap.ledger.soldInPeriod?.shown() ?: "—", MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    MoneyCard("Received this month", snap.ledger.receivedInPeriod?.shown() ?: "—", MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                SectionLabel("Recent")
                snap.ledger.entries.forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text("${formatDay(entry.date)} · ${entry.description}")
                            Muted("${typeLabel(entry.kind)} ${entry.amount.shown()}")
                            if (entry.edited) Text("Edited", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = { onView(entry.kind, entry.id) }) { Text("View") }
                    }
                    HorizontalDivider()
                }
                if (snap.ledger.entries.isEmpty()) Muted("No sales or receipts in this page.")
                PageSizeBar(page, pages, pageSize, { page = it }, { pageSize = it; page = 1 })
            }
        }
    }
}

@Composable
fun VendorDetailScreen(id: String, onBack: () -> Unit, onProfile: () -> Unit, onPay: () -> Unit, onView: (String) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableIntStateOf(1) }
    var pageSize by rememberSaveable { mutableIntStateOf(20) }
    var settling by remember { mutableStateOf(false) }
    var settleError by remember { mutableStateOf<String?>(null) }
    var settleKey by remember { mutableStateOf(newIdempotencyKey()) }
    val remote = rememberRemote("$id|$page|$pageSize") {
        VendorSnapshot(repo.party("vendor", id), repo.vendorLedger(id, "this_month", null, null, page, pageSize))
    }
    BookScreen(title = "Vendor", onBack = onBack, embedded = true) { padding ->
        RemoteBody(remote) { snap ->
            val pages = ((snap.ledger.total + pageSize - 1) / pageSize).coerceAtLeast(1)
            FormColumn(padding) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(snap.party.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onProfile) { Text("Profile") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Payable", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(snap.ledger.summary.payable.shown(), style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(onClick = onPay) { Icon(Icons.Filled.Add, contentDescription = "Record payment") }
                }
                Muted("Opening payable minus posted payments.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyCard("Paid this month", snap.ledger.summary.paidInPeriod.shown(), MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    MoneyCard("Paid till date", snap.ledger.summary.paidTillDate.shown(), MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            settling = true
                            settleError = null
                            try {
                                repo.settleVendor(id, settleKey)
                                settleKey = newIdempotencyKey()
                                remote.reload()
                            } catch (caught: Exception) {
                                settleError = caught.message
                            } finally {
                                settling = false
                            }
                        }
                    },
                    enabled = !settling && snap.ledger.summary.paidTillDate.paise != 0L,
                ) { Text(if (settling) "Settling…" else "Settle") }
                Muted("Settle sets the paid total back to zero. Past payments stay in the history, with one settled row for that total.")
                ErrorLine(settleError)
                SectionLabel("History")
                snap.ledger.transactions.forEach { payment ->
                    val settled = payment.type == "vendor_settlement" || payment.notes == "Settled"
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text("${formatDay(payment.date)} · ${if (settled) "Settled" else "Payment"} ${payment.amount.shown()}")
                            Muted("${paymentLabel(payment.paymentMethod)} · ${payment.status}")
                            if (payment.edited) Text("Edited", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = { onView(payment.id) }) { Text("View") }
                    }
                    HorizontalDivider()
                }
                if (snap.ledger.transactions.isEmpty()) Muted("No payments in this page.")
                PageSizeBar(
                    page = page,
                    pages = pages,
                    pageSize = pageSize,
                    onPage = { page = it },
                    onPageSize = {
                        pageSize = it
                        page = 1
                    },
                )
            }
        }
    }
}

@Composable
fun PartyProfileScreen(kind: String, id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val remote = rememberRemote("$kind|$id") { repo.party(kind, id) }
    BookScreen(title = "Profile", onBack = onBack, embedded = true) { padding ->
        RemoteBody(remote) { party ->
            FormColumn(padding) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(party.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    StatusPill(if (party.status == "active") "Active" else "Inactive", party.status == "active")
                }
                partyFacts(party)
                TextButton(onClick = onEdit) { Text("Edit") }
                var statusError by remember { mutableStateOf<String?>(null) }
                ErrorLine(statusError)
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val next = if (party.status == "active") "inactive" else "active"
                            repo.setPartyStatus(kind, id, next)
                            remote.reload()
                        } catch (caught: Exception) {
                            statusError = caught.message
                        }
                    }
                }) { Text(if (party.status == "active") "Mark inactive" else "Mark active") }
            }
        }
    }
}

@Composable
private fun PageSizeBar(page: Int, pages: Int, pageSize: Int, onPage: (Int) -> Unit, onPageSize: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { if (page > 1) onPage(page - 1) }, enabled = page > 1) { Text("Previous") }
            Text("$page / $pages", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { if (page < pages) onPage(page + 1) }, enabled = page < pages) { Text("Next") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 50).forEach { size ->
                FilterChip(selected = pageSize == size, onClick = { onPageSize(size) }, label = { Text("$size") })
            }
        }
        Muted("Rows per page. 20 is the usual size.")
    }
}

@Composable
private fun partyFacts(party: Party) {
    if (!party.phone.isNullOrBlank()) DetailRow("Phone", party.phone)
    if (!party.businessName.isNullOrBlank()) DetailRow("Business", party.businessName)
    if (!party.address.isNullOrBlank()) DetailRow("Address", party.address)
    if (!party.code.isNullOrBlank()) DetailRow("Code", party.code)
    if (!party.notes.isNullOrBlank()) DetailRow("Notes", party.notes)
    DetailRow("Opening balance", party.openingBalance.shown())
    if (party.createdAt != null) DetailRow("Added", formatWhen(party.createdAt))
}

private data class CustomerSnapshot(
    val party: Party,
    val ledger: com.greenworldlighting.entrybook.data.CustomerLedger,
)

private data class VendorSnapshot(
    val party: Party,
    val ledger: com.greenworldlighting.entrybook.data.VendorLedger,
)
