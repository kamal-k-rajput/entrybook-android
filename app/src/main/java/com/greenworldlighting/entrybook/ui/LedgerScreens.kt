package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.PAYMENT_METHODS
import com.greenworldlighting.entrybook.core.TX_TYPES
import com.greenworldlighting.entrybook.core.blankToNull
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.formatWhen
import com.greenworldlighting.entrybook.core.labelOf
import com.greenworldlighting.entrybook.core.newIdempotencyKey
import com.greenworldlighting.entrybook.core.normalizeRupees
import com.greenworldlighting.entrybook.core.paiseDisplay
import com.greenworldlighting.entrybook.core.paymentLabel
import com.greenworldlighting.entrybook.core.periodReady
import com.greenworldlighting.entrybook.core.reasonError
import com.greenworldlighting.entrybook.core.rupeePaise
import com.greenworldlighting.entrybook.core.saleStatusLabel
import com.greenworldlighting.entrybook.core.todayKolkata
import com.greenworldlighting.entrybook.core.typeLabel
import com.greenworldlighting.entrybook.data.AllocationRequest
import com.greenworldlighting.entrybook.data.ApiException
import com.greenworldlighting.entrybook.data.ExpenseRequest
import com.greenworldlighting.entrybook.data.InitialReceiptRequest
import com.greenworldlighting.entrybook.data.ReceiptRequest
import com.greenworldlighting.entrybook.data.SaleRequest
import com.greenworldlighting.entrybook.data.Transaction
import com.greenworldlighting.entrybook.data.VendorPaymentRequest
import com.greenworldlighting.entrybook.ui.theme.inflowColor
import com.greenworldlighting.entrybook.ui.theme.outflowColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SaleFormScreen(customerId: String, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val party = rememberRemote(customerId) { repo.party("customer", customerId) }
    var date by rememberSaveable { mutableStateOf(todayKolkata()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var invoice by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var paidNow by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("cash") }
    var confirmInactive by rememberSaveable { mutableStateOf(false) }
    var key by rememberSaveable { mutableStateOf(newIdempotencyKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "New sale", onBack = onBack, embedded = true) { padding ->
        FormColumn(padding) {
            party.value?.let { Text(it.name, style = MaterialTheme.typography.titleLarge) }
            Muted("Enter the sale, then how much was paid now. The rest stays as udhar.")
            DateField("Date", date) { date = it }
            AmountField("Sale amount", amount) { amount = it }
            AmountField("Paid now", paidNow) { paidNow = it }
            Text(udharLabel(amount, paidNow), style = MaterialTheme.typography.titleMedium)
            if (paidNow.isNotBlank()) {
                ChoiceField("Payment method", paymentLabel(method), PAYMENT_METHODS) { method = it }
            }
            OutlinedTextField(invoice, { invoice = it }, label = { Text("Invoice number (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            NotesField(notes, { notes = it })
            if (party.value?.status == "inactive") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmInactive, { confirmInactive = it })
                    Text("This customer is inactive. Record anyway.")
                }
            }
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save sale", enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val amountValue = normalizeRupees(amount)
                        val paidValue = paidNow.blankToNull()?.let { normalizeRupees(it, allowZero = true) }
                        val soldPaise = rupeePaise(amountValue)
                        val paidPaise = paidValue?.let { rupeePaise(it, allowZero = true) } ?: 0L
                        if (paidPaise > soldPaise) throw IllegalArgumentException("Paid now cannot be more than the sale")
                        val receipt = if (paidPaise > 0 && paidValue != null) {
                            InitialReceiptRequest(amount = paidValue, date = date, paymentMethod = method)
                        } else {
                            null
                        }
                        val created = repo.createSale(
                            customerId,
                            SaleRequest(
                                date = date,
                                amount = amountValue,
                                invoiceNumber = invoice.blankToNull(),
                                notes = notes.blankToNull(),
                                confirmInactive = confirmInactive.takeIf { it },
                                initialReceipt = receipt,
                            ),
                            key,
                        )
                        onCreated(created.sale.id)
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
fun ReceiptFormScreen(customerId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val party = rememberRemote(customerId) { repo.party("customer", customerId) }
    val dues = rememberRemote(customerId) { repo.dues("this_month", null, null).invoices.filter { it.customerId == customerId } }
    var date by rememberSaveable { mutableStateOf(todayKolkata()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("upi") }
    var reference by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var allowAdvance by rememberSaveable { mutableStateOf(false) }
    var confirmInactive by rememberSaveable { mutableStateOf(false) }
    var allocate by rememberSaveable { mutableStateOf(false) }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    var allocations by remember { mutableStateOf(mapOf<String, String>()) }
    var key by rememberSaveable { mutableStateOf(newIdempotencyKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "Receipt", onBack = onBack, embedded = true) { padding ->
        FormColumn(padding) {
            party.value?.let { Text(it.name, style = MaterialTheme.typography.titleLarge) }
            Muted("If you don’t choose invoices, the receipt is applied to the oldest open sales.")
            DateField("Date", date) { date = it }
            AmountField("Amount", amount) { amount = it }
            ChoiceField("Payment method", paymentLabel(method), PAYMENT_METHODS) { method = it }
            OutlinedTextField(reference, { reference = it }, label = { Text("Reference") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            NotesField(notes, { notes = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(allowAdvance, { allowAdvance = it })
                Text("Allow more than the outstanding balance")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(allocate, { allocate = it })
                Text("Choose invoices")
            }
            if (allocate) {
                dues.value.orEmpty().forEach { invoice ->
                    val checked = invoice.saleId in chosen
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, { on ->
                            chosen = if (on) chosen + invoice.saleId else chosen - invoice.saleId
                            if (on && allocations[invoice.saleId].isNullOrBlank()) {
                                allocations = allocations + (invoice.saleId to invoice.remaining.rupees)
                            }
                        })
                        Column(Modifier.weight(1f)) {
                            Text(invoice.invoiceNumber ?: "Sale ${formatDay(invoice.date)}")
                            Muted("Remaining ${invoice.remaining.shown()}")
                        }
                    }
                    if (checked) {
                        AmountField("Amount for this invoice", allocations[invoice.saleId].orEmpty()) {
                            allocations = allocations + (invoice.saleId to it)
                        }
                    }
                }
                if (dues.value?.isEmpty() == true) Muted("No open invoices in the current month’s dues.")
            }
            if (party.value?.status == "inactive") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmInactive, { confirmInactive = it })
                    Text("This customer is inactive. Record anyway.")
                }
            }
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save receipt", enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val lines = if (allocate) {
                            chosen.map { saleId ->
                                AllocationRequest(saleId, normalizeRupees(allocations[saleId].orEmpty()))
                            }.ifEmpty { null }
                        } else {
                            null
                        }
                        repo.createReceipt(
                            customerId,
                            ReceiptRequest(
                                amount = normalizeRupees(amount),
                                date = date,
                                paymentMethod = method,
                                reference = reference.blankToNull(),
                                notes = notes.blankToNull(),
                                allowAdvance = allowAdvance.takeIf { it },
                                confirmInactive = confirmInactive.takeIf { it },
                                allocations = lines,
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
fun PaymentFormScreen(vendorId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val party = rememberRemote(vendorId) { repo.party("vendor", vendorId) }
    val categories = rememberRemote(vendorId) { repo.categories("business", true) }
    var date by rememberSaveable { mutableStateOf(todayKolkata()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("upi") }
    var reference by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var confirmInactive by rememberSaveable { mutableStateOf(false) }
    var key by rememberSaveable { mutableStateOf(newIdempotencyKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "Vendor payment", onBack = onBack, embedded = true) { padding ->
        FormColumn(padding) {
            party.value?.let { Text(it.name, style = MaterialTheme.typography.titleLarge) }
            DateField("Date", date) { date = it }
            AmountField("Amount", amount) { amount = it }
            ChoiceField("Payment method", paymentLabel(method), PAYMENT_METHODS) { method = it }
            val options = listOf("" to "No category") + categories.value.orEmpty().map { it.id to it.name }
            ChoiceField("Category", options.firstOrNull { it.first == categoryId }?.second ?: "No category", options) { categoryId = it }
            OutlinedTextField(reference, { reference = it }, label = { Text("Reference") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            NotesField(notes, { notes = it })
            if (party.value?.status == "inactive") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmInactive, { confirmInactive = it })
                    Text("This vendor is inactive. Pay anyway.")
                }
            }
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save payment", enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val result = repo.payVendor(
                            vendorId,
                            VendorPaymentRequest(
                                amount = normalizeRupees(amount),
                                date = date,
                                paymentMethod = method,
                                reference = reference.blankToNull(),
                                notes = notes.blankToNull(),
                                categoryId = categoryId.blankToNull(),
                                confirmInactive = confirmInactive.takeIf { it },
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
fun ExpenseFormScreen(onBack: () -> Unit, onDone: () -> Unit, personalOnly: Boolean = false, businessOnly: Boolean = false) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val categories = rememberRemote(Unit) { repo.categories(active = true) }
    var expenseType by rememberSaveable { mutableStateOf(if (personalOnly) "personal" else "business") }
    var date by rememberSaveable { mutableStateOf(todayKolkata()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("upi") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf(newIdempotencyKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val matching = categories.value.orEmpty().filter { it.expenseType == expenseType && it.active }
    LaunchedEffect(matching.map { it.id }, expenseType) {
        if (matching.none { it.id == categoryId }) categoryId = matching.firstOrNull()?.id.orEmpty()
    }
    val expenseTitle = when {
        personalOnly -> "Personal expense"
        businessOnly -> "Business expense"
        else -> "Expense"
    }
    BookScreen(title = expenseTitle, onBack = onBack, embedded = true) { padding ->
        FormColumn(padding) {
            Muted(
                when {
                    personalOnly -> "This is spending from your own pocket. The month total is on Home."
                    businessOnly -> "This is a shop expense and counts in cash out."
                    else -> "Personal expenses stay out of business operating totals and still count in cash out."
                },
            )
            if (!personalOnly && !businessOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(expenseType == "business", { expenseType = "business" }, label = { Text("Business") })
                    FilterChip(expenseType == "personal", { expenseType = "personal" }, label = { Text("Personal") })
                }
            }
            DateField("Date", date) { date = it }
            AmountField("Amount", amount) { amount = it }
            ChoiceField("Payment method", paymentLabel(method), PAYMENT_METHODS) { method = it }
            if (matching.isEmpty()) {
                Muted("No active ${expenseType} categories. Add one from More.")
            } else {
                ChoiceField("Category", matching.firstOrNull { it.id == categoryId }?.name ?: "Category", matching.map { it.id to it.name }) {
                    categoryId = it
                }
            }
            NotesField(notes, { notes = it })
            ErrorLine(error)
            PrimaryButton(if (busy) "Saving…" else "Save expense", enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        if (categoryId.isBlank()) throw IllegalArgumentException("Choose a category")
                        repo.createExpense(
                            ExpenseRequest(
                                expenseType = expenseType,
                                amount = normalizeRupees(amount),
                                date = date,
                                categoryId = categoryId,
                                paymentMethod = method,
                                notes = notes.blankToNull(),
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
fun TransactionsScreen(onOpen: (String) -> Unit, onExpense: () -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    var type by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        delay(300)
        debounced = query
    }
    LaunchedEffect(period, from, to, type, status, debounced) { page = 1 }
    val ready = periodReady(period, from, to)
    LaunchedEffect(period, from, to, type, status, debounced, page, ready) {
        if (!ready) return@LaunchedEffect
        loading = true
        error = null
        try {
            val result = repo.transactions(
                period,
                from.takeIf { period == "custom" },
                to.takeIf { period == "custom" },
                type,
                status,
                null,
                debounced,
                page,
            )
            items = if (page == 1) result.items else items + result.items
            total = result.total
        } catch (caught: Exception) {
            error = caught.message
        } finally {
            loading = false
        }
    }
    BookScreen(title = "Cash book", embedded = true, actions = {
        IconButton(onClick = onExpense) { Icon(Icons.Filled.Add, contentDescription = "Add expense") }
        IconButton(onClick = {
            if (!ready || exporting) return@IconButton
            scope.launch {
                exporting = true
                try {
                    val bytes = repo.exportTransactions(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" }, type, status, debounced)
                    val name = if (period == "custom") "transactions-$from-$to.csv" else "transactions-$period.csv"
                    shareCsv(context, bytes, name)
                } catch (caught: Exception) {
                    error = caught.message
                } finally {
                    exporting = false
                }
            }
        }) { Icon(Icons.Filled.FileDownload, contentDescription = "Export CSV") }
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
            item { ChoiceField("Type", labelOf(TX_TYPES, type), TX_TYPES) { type = it } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to "All", "posted" to "Posted", "reversed" to "Reversed").forEach { (id, label) ->
                        FilterChip(selected = status == id, onClick = { status = id }, label = { Text(label) })
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, label = { Text("Reference or notes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (!ready) item { Muted("Choose a start and end date.") }
            if (error != null) item { ErrorLine(error) }
            if (ready && !loading && items.isEmpty() && error == null) item { Muted("No transactions in this period.") }
            items(items, key = { it.id }) { tx ->
                TransactionRow(tx) { onOpen(tx.id) }
            }
            if (loading) item { LoadingBox() }
            if (!loading && items.size < total) item { TextButton(onClick = { page += 1 }) { Text("Load more") } }
        }
    }
}

private fun udharLabel(sale: String, paid: String): String {
    return try {
        if (sale.isBlank()) return "Udhar"
        val sold = rupeePaise(sale)
        val paidPaise = if (paid.isBlank()) 0L else rupeePaise(paid, allowZero = true)
        if (paidPaise > sold) "Paid now is more than the sale" else "Udhar ${paiseDisplay(sold - paidPaise)}"
    } catch (_: Exception) {
        "Udhar"
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val tint = when {
        tx.status == "reversed" -> MaterialTheme.colorScheme.onSurfaceVariant
        tx.direction == "in" -> inflowColor()
        else -> outflowColor()
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(Modifier)) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("${formatDay(tx.date)} · ${typeLabel(tx.type)}", color = MaterialTheme.colorScheme.onSurface)
                Muted(listOfNotNull(tx.partyName, paymentLabel(tx.paymentMethod), tx.status).joinToString(" · "))
            }
            Text(tx.amount.shown(), color = tint, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionDetailScreen(id: String, onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val remote = rememberRemote(id) { repo.transaction(id) }
    var reason by rememberSaveable { mutableStateOf("") }
    var ask by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "Transaction", onBack = onBack, embedded = true) { padding ->
        RemoteBody(remote) { tx ->
            FormColumn(padding) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tx.amount.shown(), style = MaterialTheme.typography.headlineMedium, color = if (tx.direction == "in") inflowColor() else outflowColor())
                    if (tx.edited) StatusPill("Edited", active = false)
                }
                if (tx.history.isNotEmpty()) {
                    SectionLabel("Amount history")
                    tx.history.forEach { revision ->
                        DetailRow("Previous", "${revision.amount.shown()} · ${formatWhen(revision.at)}")
                    }
                    DetailRow("Current", tx.amount.shown())
                }
                DetailRow("Type", typeLabel(tx.type))
                DetailRow("Date", formatDay(tx.date))
                DetailRow("Method", paymentLabel(tx.paymentMethod))
                DetailRow("Status", tx.status)
                if (!tx.partyName.isNullOrBlank()) DetailRow("Party", tx.partyName)
                if (!tx.reference.isNullOrBlank()) DetailRow("Reference", tx.reference)
                if (!tx.notes.isNullOrBlank()) DetailRow("Notes", tx.notes)
                if (!tx.reversalReason.isNullOrBlank()) DetailRow("Reversal reason", tx.reversalReason)
                if (tx.reversedAt != null) DetailRow("Reversed", formatWhen(tx.reversedAt))
                if (tx.createdAt != null) DetailRow("Entered", formatWhen(tx.createdAt))
                ErrorLine(error)
                if (tx.status == "posted") {
                    var nextAmount by rememberSaveable { mutableStateOf("") }
                    AmountField("New amount", nextAmount) { nextAmount = it }
                    PrimaryButton(if (busy) "Saving…" else "Save amount", enabled = !busy) {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                repo.editTransaction(id, normalizeRupees(nextAmount))
                                nextAmount = ""
                                remote.reload()
                            } catch (caught: Exception) {
                                error = caught.message
                            } finally {
                                busy = false
                            }
                        }
                    }
                    PrimaryButton("Reverse") { ask = true }
                }
            }
        }
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { if (!busy) ask = false },
            title = { Text("Reverse this entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The row stays in history and drops out of totals. Reversing a receipt clears its sale allocations.")
                    OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                    ErrorLine(error)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val problem = reasonError(reason)
                    if (problem != null) {
                        error = problem
                        return@TextButton
                    }
                    scope.launch {
                        busy = true
                        try {
                            repo.reverse(id, reason)
                            ask = false
                            remote.reload()
                        } catch (caught: Exception) {
                            error = caught.message
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("Reverse") }
            },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SaleDetailScreen(id: String, onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val remote = rememberRemote(id) { repo.sale(id) }
    var reason by rememberSaveable { mutableStateOf("") }
    var ask by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "Sale", onBack = onBack, embedded = true) { padding ->
        RemoteBody(remote) { sale ->
            FormColumn(padding) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sale.netAmount.shown(), style = MaterialTheme.typography.headlineMedium)
                    if (sale.edited) StatusPill("Edited", active = false)
                }
                Text(sale.invoiceNumber ?: "Sale", style = MaterialTheme.typography.titleMedium)
                StatusPill(saleStatusLabel(sale.status), sale.status != "void")
                DetailRow("Date", formatDay(sale.date))
                if (sale.history.isNotEmpty()) {
                    SectionLabel("Amount history")
                    sale.history.forEach { revision ->
                        DetailRow("Previous", "${revision.amount.shown()} · ${formatWhen(revision.at)}")
                    }
                    DetailRow("Current", sale.netAmount.shown())
                }
                if (sale.remaining != null) DetailRow("Udhar", sale.remaining.shown())
                if (sale.allocated != null) DetailRow("Received", sale.allocated.shown())
                if (sale.dueDate != null) DetailRow("Due", formatDay(sale.dueDate))
                if (!sale.notes.isNullOrBlank()) DetailRow("Notes", sale.notes)
                if (!sale.voidReason.isNullOrBlank()) DetailRow("Void reason", sale.voidReason)
                ErrorLine(error)
                if (sale.status != "void") {
                    var nextAmount by rememberSaveable { mutableStateOf("") }
                    AmountField("New sale amount", nextAmount) { nextAmount = it }
                    PrimaryButton(if (busy) "Saving…" else "Save amount", enabled = !busy) {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                repo.editSale(id, normalizeRupees(nextAmount))
                                nextAmount = ""
                                remote.reload()
                            } catch (caught: Exception) {
                                error = caught.message
                            } finally {
                                busy = false
                            }
                        }
                    }
                    Muted("The new amount cannot be less than what has already been received.")
                    PrimaryButton("Void sale") { ask = true }
                }
            }
        }
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { if (!busy) ask = false },
            title = { Text("Void this sale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                    ErrorLine(error)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val problem = reasonError(reason)
                    if (problem != null) {
                        error = problem
                        return@TextButton
                    }
                    scope.launch {
                        busy = true
                        try {
                            repo.voidSale(id, reason)
                            ask = false
                            remote.reload()
                        } catch (caught: Exception) {
                            error = caught.message
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("Void") }
            },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancel") } },
        )
    }
}
