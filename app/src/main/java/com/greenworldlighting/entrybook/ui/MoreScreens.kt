package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenworldlighting.entrybook.core.formatWhen
import com.greenworldlighting.entrybook.core.subscriptionLabel
import com.greenworldlighting.entrybook.data.AuditEntry
import com.greenworldlighting.entrybook.data.Notice
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Composable
fun MoreScreen(
    onReports: () -> Unit,
    onNotifications: () -> Unit,
    onBusiness: () -> Unit,
    onCategories: () -> Unit,
    onAudit: () -> Unit,
    onLogout: () -> Unit,
) {
    val session = LocalSession.current
    BookScreen(title = "More", embedded = true) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(session.account?.user?.name ?: "Shop", style = MaterialTheme.typography.headlineMedium)
            Muted(session.account?.user?.email ?: "")
            session.account?.subscription?.let { sub ->
                StatusPill(subscriptionLabel(sub.status), sub.active)
            }
            listOf(
                "Reports" to onReports,
                "Notifications" to onNotifications,
                "Business profile" to onBusiness,
                "Expense categories" to onCategories,
                "Audit log" to onAudit,
            ).forEach { (label, action) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = action),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(label, modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
            TextButton(onClick = { session.switchUsage("personal") }) { Text("Switch to personal") }
            Muted("Switching does not change the end date. An administrator can extend it.")
            TextButton(onClick = onLogout) { Text("Sign out") }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, embedded: Boolean) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<Notice>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(page, reload) {
        loading = true
        error = null
        try {
            val result = repo.notifications(page)
            items = if (page == 1) result.items else items + result.items
            total = result.total
        } catch (caught: Exception) {
            error = caught.message
        } finally {
            loading = false
        }
    }
    BookScreen(title = "Notifications", onBack = onBack, embedded = embedded, actions = {
        TextButton(onClick = {
            scope.launch {
                try {
                    repo.markAllRead()
                    page = 1
                    reload += 1
                } catch (caught: Exception) {
                    error = caught.message
                }
            }
        }) { Text("Read all", color = com.greenworldlighting.entrybook.ui.theme.BrandPaper) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (error != null) item { ErrorLine(error) }
            if (!loading && items.isEmpty()) item { Muted("No notifications.") }
            items(items, key = { it.id }) { notice ->
                Column(Modifier.fillMaxWidth().clickable {
                    if (notice.readAt == null) {
                        scope.launch {
                            try {
                                repo.markRead(notice.id)
                                items = items.map { if (it.id == notice.id) it.copy(readAt = it.readAt ?: "") else it }
                            } catch (caught: Exception) {
                                error = caught.message
                            }
                        }
                    }
                }.padding(vertical = 6.dp)) {
                    Text(notice.title, style = MaterialTheme.typography.titleMedium)
                    Text(notice.body)
                    Muted("${formatWhen(notice.createdAt)} · ${if (notice.readAt == null) "Unread" else "Read"}")
                    HorizontalDivider()
                }
            }
            if (loading) item { LoadingBox() }
            if (!loading && items.size < total) item { TextButton(onClick = { page += 1 }) { Text("Load more") } }
        }
    }
}

@Composable
fun BusinessScreen(onBack: () -> Unit, embedded: Boolean) {
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    val remote = rememberRemote(Unit) { repo.business() }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(remote.value?.id) {
        val business = remote.value ?: return@LaunchedEffect
        if (!loaded) {
            name = business.name
            phone = business.phone.orEmpty()
            address = business.address.orEmpty()
            loaded = true
        }
    }
    BookScreen(title = "Business", onBack = onBack, embedded = embedded) { padding ->
        when {
            remote.loading && remote.value == null -> LoadingBox()
            remote.error != null && remote.value == null -> ErrorPane(remote.error, remote.reload)
            else -> FormColumn(padding) {
                val business = remote.value
                OutlinedTextField(name, { name = it }, label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                if (business != null) {
                    DetailRow("Currency", business.currency)
                    DetailRow("Timezone", business.timezone)
                }
                session.account?.subscription?.let { sub ->
                    DetailRow("Subscription", subscriptionLabel(sub.status))
                    if (sub.endDate != null) DetailRow("Ends", com.greenworldlighting.entrybook.core.formatDay(sub.endDate))
                }
                Muted("Currency stays INR. The timezone stays with the shop.")
                ErrorLine(error)
                PrimaryButton(if (busy) "Saving…" else "Save", enabled = !busy && name.trim().length >= 2) {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            repo.updateBusiness(name, phone, address)
                            session.refreshAccount()
                            loaded = false
                            remote.reload()
                        } catch (caught: Exception) {
                            error = caught.message
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val remote = rememberRemote(reload) { repo.categories() }
    var adding by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var expenseType by rememberSaveable { mutableStateOf("business") }
    var error by remember { mutableStateOf<String?>(null) }
    BookScreen(title = "Categories", onBack = onBack, embedded = true, actions = {
        TextButton(onClick = { adding = true }) { Text("Add", color = com.greenworldlighting.entrybook.ui.theme.BrandPaper) }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (remote.loading && remote.value == null) item { LoadingBox() }
            if (remote.error != null && remote.value == null) {
                item { ErrorPane(remote.error ?: "Something went wrong", remote.reload) }
            }
            if (error != null) item { ErrorLine(error) }
            val groups = remote.value.orEmpty().groupBy { it.expenseType }
            val listScope = this
            listOf("business" to "Business", "personal" to "Personal").forEach { (type, label) ->
                listScope.item { SectionLabel(label) }
                listScope.items(groups[type].orEmpty(), key = { it.id }) { category ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Muted(if (category.active) "Active" else "Inactive")
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    repo.updateCategory(category.id, active = !category.active)
                                    reload += 1
                                } catch (caught: Exception) {
                                    error = caught.message
                                }
                            }
                        }) { Text(if (category.active) "Deactivate" else "Activate") }
                    }
                }
            }
        }
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(expenseType == "business", { expenseType = "business" }, label = { Text("Business") })
                        FilterChip(expenseType == "personal", { expenseType = "personal" }, label = { Text("Personal") })
                    }
                    ErrorLine(error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            if (name.trim().length < 2) throw IllegalArgumentException("Enter a name")
                            repo.createCategory(name, expenseType)
                            name = ""
                            adding = false
                            reload += 1
                        } catch (caught: Exception) {
                            error = caught.message
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun AuditScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    var page by rememberSaveable { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<AuditEntry>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<AuditEntry?>(null) }
    LaunchedEffect(page) {
        loading = true
        try {
            val result = repo.audit(page)
            items = if (page == 1) result.items else items + result.items
            total = result.total
            error = null
        } catch (caught: Exception) {
            error = caught.message
        } finally {
            loading = false
        }
    }
    BookScreen(title = "Audit log", onBack = onBack, embedded = true) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Muted("Financial corrections stay here. The original row is kept.") }
            if (error != null) item { ErrorLine(error) }
            if (!loading && items.isEmpty()) item { Muted("No audit entries yet.") }
            items(items, key = { it.id }) { entry ->
                Column(Modifier.fillMaxWidth().clickable { open = entry }.padding(vertical = 6.dp)) {
                    Text("${entry.action} · ${entry.entityType}", style = MaterialTheme.typography.titleMedium)
                    Muted(formatWhen(entry.createdAt))
                    HorizontalDivider()
                }
            }
            if (loading) item { LoadingBox() }
            if (!loading && items.size < total) item { TextButton(onClick = { page += 1 }) { Text("Load more") } }
        }
    }
    open?.let { entry ->
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text("${entry.action} ${entry.entityType}") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Before", style = MaterialTheme.typography.labelLarge)
                    Text(prettyJson(entry.before), style = MaterialTheme.typography.bodySmall)
                    Text("After", style = MaterialTheme.typography.labelLarge)
                    Text(prettyJson(entry.after), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Close") } },
        )
    }
}

private val auditJson = Json { prettyPrint = true }

private fun prettyJson(element: JsonElement?): String {
    if (element == null || element is JsonNull) return "—"
    return auditJson.encodeToString(JsonElement.serializer(), element)
}
