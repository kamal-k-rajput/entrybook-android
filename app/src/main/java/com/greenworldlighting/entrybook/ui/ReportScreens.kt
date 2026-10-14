package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.bucketLabel
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.periodReady
import com.greenworldlighting.entrybook.core.todayKolkata
import com.greenworldlighting.entrybook.core.typeLabel
import com.greenworldlighting.entrybook.ui.theme.inflowColor
import com.greenworldlighting.entrybook.ui.theme.outflowColor

@Composable
fun ReportsScreen(onOpen: (String) -> Unit) {
    BookScreen(title = "Reports", embedded = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Muted("Sales are not treated as cash. Cash in is customer receipts.")
            listOf(
                "cash" to "Cash movement",
                "customers" to "Customers",
                "vendors" to "Vendors",
                "dues" to "Dues",
                "expenses" to "Personal expenses",
            ).forEach { (id, label) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(id) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(label, modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    kind: String,
    onBack: () -> Unit,
    onSale: (String) -> Unit,
    onCustomer: (String) -> Unit,
    onVendor: (String) -> Unit,
) {
    val title = when (kind) {
        "cash" -> "Cash movement"
        "customers" -> "Customers"
        "vendors" -> "Vendors"
        "dues" -> "Dues"
        "expenses" -> "Personal expenses"
        else -> "Report"
    }
    BookScreen(title = title, onBack = onBack, embedded = true) { padding ->
        when (kind) {
            "cash" -> CashReport(padding)
            "customers" -> CustomerReportList(padding, onCustomer)
            "vendors" -> VendorReportList(padding, onVendor)
            "dues" -> DuesReportList(padding, onSale)
            "expenses" -> ExpenseReportList(padding)
            else -> Muted("Unknown report")
        }
    }
}

@Composable
private fun CashReport(padding: PaddingValues) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.cashMovement(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
        item {
            if (!ready) Muted("Choose a start and end date.")
            else RemoteBody(remote) { cash ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyCard("In", cash.inflow.shown(), inflowColor())
                    MoneyCard("Out", cash.outflow.shown(), outflowColor())
                    MoneyCard("Net", cash.net.shown(), if (cash.net.paise >= 0) inflowColor() else outflowColor(), cash.note)
                    DetailRow("Personal expenses", cash.personalExpense.shown())
                    DetailRow("Business outflow", cash.businessOutflow.shown())
                    Muted("${formatDay(cash.period.from)} – ${formatDay(cash.period.to)}")
                }
            }
        }
    }
}

@Composable
private fun CustomerReportList(padding: PaddingValues, onCustomer: (String) -> Unit) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.customerReport(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    val rows = remote.value?.items.orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
        item {
            when {
                !ready -> Muted("Choose a start and end date.")
                remote.loading && remote.value == null -> LoadingBox()
                remote.error != null && remote.value == null -> ErrorPane(remote.error, remote.reload)
                remote.value != null -> Muted("${formatDay(remote.value.period.from)} – ${formatDay(remote.value.period.to)}")
            }
        }
        items(rows, key = { it.customer.id }) { row ->
            Column(Modifier.fillMaxWidth().clickable { onCustomer(row.customer.id) }.padding(vertical = 8.dp)) {
                Text(row.customer.name, style = MaterialTheme.typography.titleMedium)
                Muted("Sales ${row.salesInPeriod.shown()} · Received ${row.receivedInPeriod.shown()}")
                Text("Outstanding ${row.outstanding.shown()}")
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VendorReportList(padding: PaddingValues, onVendor: (String) -> Unit) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.vendorReport(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    val rows = remote.value?.items.orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
        item {
            when {
                !ready -> Muted("Choose a start and end date.")
                remote.loading && remote.value == null -> LoadingBox()
                remote.error != null && remote.value == null -> ErrorPane(remote.error, remote.reload)
                else -> Unit
            }
        }
        items(rows, key = { it.vendor.id }) { row ->
            Column(Modifier.fillMaxWidth().clickable { onVendor(row.vendor.id) }.padding(vertical = 8.dp)) {
                Text(row.vendor.name, style = MaterialTheme.typography.titleMedium)
                Muted("Paid this period ${row.paidInPeriod.shown()} · Paid till date ${row.paidTillDate.shown()}")
                Text("Payable ${row.payable.shown()}")
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DuesReportList(padding: PaddingValues, onSale: (String) -> Unit) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.dues(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    val invoices = remote.value?.invoices.orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
        item {
            when {
                !ready -> Muted("Choose a start and end date.")
                remote.loading && remote.value == null -> LoadingBox()
                remote.error != null && remote.value == null -> ErrorPane(remote.error, remote.reload)
                remote.value != null -> Muted("Open and partial invoices as of ${formatDay(remote.value.asOf)}. Ageing uses today in Asia/Kolkata.")
            }
        }
        if (remote.value != null && invoices.isEmpty()) item { Muted("No open dues.") }
        items(invoices, key = { it.saleId }) { invoice ->
            Column(Modifier.fillMaxWidth().clickable { onSale(invoice.saleId) }.padding(vertical = 8.dp)) {
                Text(invoice.customerName ?: "Customer", style = MaterialTheme.typography.titleMedium)
                Text(invoice.remaining.shown())
                Muted("${invoice.invoiceNumber ?: "No invoice"} · ${formatDay(invoice.date)} · due ${formatDay(invoice.dueDate)} · ${bucketLabel(invoice.bucket)}")
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ExpenseReportList(padding: PaddingValues) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$period|$from|$to", ready) {
        repo.expenseReport(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
        item {
            if (!ready) Muted("Choose a start and end date.")
            else RemoteBody(remote) { report ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyCard("Personal total", report.total.shown(), outflowColor(), report.note)
                    report.items.forEach { row -> DetailRow(row.categoryName, row.total.shown()) }
                    if (report.items.isEmpty()) Muted("No personal expenses in this period.")
                }
            }
        }
    }
}

@Composable
fun StatementScreen(customerId: String, onBack: () -> Unit) {
    val repo = LocalRepo.current
    var period by rememberSaveable { mutableStateOf("this_month") }
    var from by rememberSaveable { mutableStateOf(todayKolkata()) }
    var to by rememberSaveable { mutableStateOf(todayKolkata()) }
    val ready = periodReady(period, from, to)
    val remote = rememberRemote("$customerId|$period|$from|$to", ready) {
        repo.statement(customerId, period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" })
    }
    val entries = remote.value?.entries.orEmpty()
    BookScreen(title = "Statement", onBack = onBack, embedded = true) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PeriodPicker(period, from, to, { period = it }, { from = it }, { to = it }) }
            if (!ready) {
                item { Muted("Choose a start and end date.") }
            } else {
                item {
                    when {
                        remote.loading && remote.value == null -> LoadingBox()
                        remote.error != null && remote.value == null -> ErrorPane(remote.error, remote.reload)
                        remote.value != null -> {
                            val statement = remote.value
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(statement.customer.name, style = MaterialTheme.typography.titleLarge)
                                DetailRow("Opening", statement.openingBalance.shown())
                                DetailRow("Outstanding", statement.outstanding.shown())
                                Muted("Opening is the balance brought forward to the day before the period.")
                            }
                        }
                    }
                }
                items(entries, key = { "${it.kind}-${it.id}-${it.date}" }) { entry ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${formatDay(entry.date)} · ${entry.description}")
                        Muted("${typeLabel(entry.kind)} ${entry.amount.shown()} · balance ${entry.balance.shown()}")
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun listPadding(padding: PaddingValues) = PaddingValues(
    start = 20.dp,
    end = 20.dp,
    top = padding.calculateTopPadding() + 12.dp,
    bottom = padding.calculateBottomPadding() + 24.dp,
)
