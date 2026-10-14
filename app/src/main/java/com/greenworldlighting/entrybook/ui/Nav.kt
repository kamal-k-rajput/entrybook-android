package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.greenworldlighting.entrybook.ui.theme.BrandGreen
import com.greenworldlighting.entrybook.ui.theme.BrandPaper

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("customers", "Customers", Icons.Filled.People),
    Tab("vendors", "Vendors", Icons.Filled.LocalShipping),
    Tab("cash", "Cash", Icons.Filled.AccountBalanceWallet),
    Tab("more", "More", Icons.Filled.Menu),
)

@Composable
fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }
    Scaffold(
        containerColor = BrandGreen,
        bottomBar = { if (showBar) EntryBar(nav, route) },
        floatingActionButton = {
            if (showBar) {
                QuickAdd(shop = true, onPersonal = { nav.navigate("expense/personal") }, onBusiness = { nav.navigate("expense/business") }, onVendor = { nav.navigate("vendors/pick/pay") })
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    onNotifications = { nav.navigate("notifications") },
                    onSale = { nav.navigate("customers/pick/sale") },
                    onReceipt = { nav.navigate("customers/pick/receipt") },
                    onPayment = { nav.navigate("vendors/pick/pay") },
                    onExpense = { nav.navigate("expense/new") },
                    onReports = { nav.navigate("reports") },
                )
            }
            composable("customers") {
                PartyListScreen("customer", "", null, { nav.navigate("customer/new") }, { nav.navigate("customer/${it.id}") }, { nav.navigate("customer/${it.id}") }, showBalance = true)
            }
            composable("customers/pick/{purpose}") { back ->
                val purpose = back.arguments?.getString("purpose").orEmpty()
                PartyListScreen(
                    kind = "customer",
                    purpose = purpose,
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("customer/new") },
                    onOpen = { party ->
                        when (purpose) {
                            "sale" -> nav.navigate("customer/${party.id}/sale")
                            "receipt" -> nav.navigate("customer/${party.id}/receipt")
                            else -> nav.navigate("customer/${party.id}")
                        }
                    },
                    onDetails = { nav.navigate("customer/${it.id}") },
                    showBalance = true,
                )
            }
            composable("customer/new") {
                PartyFormScreen("customer", null, { nav.popBackStack() }) { id ->
                    nav.navigate("customer/$id") { popUpTo("customer/new") { inclusive = true } }
                }
            }
            composable("customer/{id}") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                CustomerDetailScreen(
                    id,
                    { nav.popBackStack() },
                    { nav.navigate("customer/$id/profile") },
                    { nav.navigate("customer/$id/sale") },
                    { nav.navigate("customer/$id/receipt") },
                    { kind, entryId ->
                        if (kind == "sale") nav.navigate("sale/$entryId") else nav.navigate("transaction/$entryId")
                    },
                )
            }
            composable("customer/{id}/profile") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyProfileScreen("customer", id, { nav.popBackStack() }, { nav.navigate("customer/$id/edit") })
            }
            composable("customer/{id}/edit") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyFormScreen("customer", id, { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("customer/{id}/sale") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                SaleFormScreen(id, { nav.popBackStack() }) { saleId ->
                    nav.popBackStack()
                    nav.navigate("sale/$saleId")
                }
            }
            composable("customer/{id}/receipt") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                ReceiptFormScreen(id, { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("customer/{id}/statement") { back ->
                StatementScreen(back.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("vendors") {
                PartyListScreen("vendor", "", null, { nav.navigate("vendor/new") }, { nav.navigate("vendor/${it.id}") }, { nav.navigate("vendor/${it.id}") }, showPaid = true)
            }
            composable("vendors/pick/{purpose}") {
                PartyListScreen(
                    kind = "vendor",
                    purpose = "pay",
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("vendor/new") },
                    onOpen = { nav.navigate("vendor/${it.id}/pay") },
                    onDetails = { nav.navigate("vendor/${it.id}") },
                    showPaid = true,
                )
            }
            composable("vendor/new") {
                PartyFormScreen("vendor", null, { nav.popBackStack() }) { id ->
                    nav.navigate("vendor/$id") { popUpTo("vendor/new") { inclusive = true } }
                }
            }
            composable("vendor/{id}") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                VendorDetailScreen(
                    id,
                    { nav.popBackStack() },
                    { nav.navigate("vendor/$id/profile") },
                    { nav.navigate("vendor/$id/pay") },
                    { nav.navigate("transaction/$it") },
                )
            }
            composable("vendor/{id}/profile") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyProfileScreen("vendor", id, { nav.popBackStack() }, { nav.navigate("vendor/$id/edit") })
            }
            composable("vendor/{id}/edit") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyFormScreen("vendor", id, { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("vendor/{id}/pay") { back ->
                PaymentFormScreen(back.arguments?.getString("id").orEmpty(), { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("cash") {
                TransactionsScreen({ nav.navigate("transaction/$it") }, { nav.navigate("expense/new") })
            }
            composable("expense/new") {
                ExpenseFormScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable("expense/personal") {
                ExpenseFormScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }, personalOnly = true)
            }
            composable("expense/business") {
                ExpenseFormScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }, businessOnly = true)
            }
            composable("transaction/{id}") { back ->
                TransactionDetailScreen(back.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("sale/{id}") { back ->
                SaleDetailScreen(back.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("reports") { ReportsScreen { nav.navigate("reports/$it") } }
            composable("reports/{kind}") { back ->
                val kind = back.arguments?.getString("kind").orEmpty()
                ReportDetailScreen(
                    kind,
                    { nav.popBackStack() },
                    { nav.navigate("sale/$it") },
                    { nav.navigate("customer/$it") },
                    { nav.navigate("vendor/$it") },
                )
            }
            composable("notifications") { NotificationsScreen({ nav.popBackStack() }, embedded = true) }
            composable("business") { BusinessScreen({ nav.popBackStack() }, embedded = true) }
            composable("categories") { CategoriesScreen { nav.popBackStack() } }
            composable("audit") { AuditScreen { nav.popBackStack() } }
            composable("more") {
                val logout = LocalSession.current.logout
                MoreScreen(
                    onReports = { nav.navigate("reports") },
                    onNotifications = { nav.navigate("notifications") },
                    onBusiness = { nav.navigate("business") },
                    onCategories = { nav.navigate("categories") },
                    onAudit = { nav.navigate("audit") },
                    onLogout = logout,
                )
            }
        }
    }
}

@Composable
private fun QuickAdd(
    shop: Boolean,
    onPersonal: () -> Unit,
    onBusiness: () -> Unit = {},
    onVendor: () -> Unit = {},
    onPeople: () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (open) {
            AddChoice("Personal expense") { open = false; onPersonal() }
            if (shop) {
                AddChoice("Business expense") { open = false; onBusiness() }
                AddChoice("Vendor expense") { open = false; onVendor() }
            } else {
                AddChoice("People transaction") { open = false; onPeople() }
            }
        }
        FloatingActionButton(
            onClick = { open = !open },
            containerColor = BrandGreen,
            contentColor = BrandPaper,
        ) {
            Icon(if (open) Icons.Filled.Close else Icons.Filled.Add, contentDescription = if (open) "Close" else "Add")
        }
    }
}

@Composable
private fun AddChoice(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EntryBar(nav: NavHostController, route: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(18.dp)) },
                label = {
                    Text(tab.label, fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                },
            )
        }
    }
}

@Composable
fun PersonalNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val personalTabs = listOf(
        Tab("home", "Home", Icons.Filled.Home),
        Tab("people", "People", Icons.Filled.People),
        Tab("spend", "Spend", Icons.Filled.AccountBalanceWallet),
        Tab("more", "More", Icons.Filled.Menu),
    )
    val showBar = route in personalTabs.map { it.route }
    Scaffold(
        containerColor = BrandGreen,
        floatingActionButton = {
            if (showBar) {
                QuickAdd(shop = false, onPersonal = { nav.navigate("expense/new") }, onPeople = { nav.navigate("people/pick/transfer") })
            }
        },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    personalTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                PersonalHomeScreen(
                    onPerson = { nav.navigate("people") },
                    onTransfer = { nav.navigate("people/pick/transfer") },
                    onExpense = { nav.navigate("expense/new") },
                )
            }
            composable("people") {
                PartyListScreen("customer", "", null, { nav.navigate("person/new") }, { nav.navigate("person/${it.id}") }, { nav.navigate("person/${it.id}") }, "Friends", showNet = true)
            }
            composable("people/pick/{purpose}") {
                PartyListScreen(
                    kind = "customer",
                    purpose = "",
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("person/new") },
                    onOpen = { nav.navigate("person/${it.id}/transfer") },
                    onDetails = { nav.navigate("person/${it.id}") },
                    listTitle = "Friends",
                    showNet = true,
                )
            }
            composable("person/new") {
                PartyFormScreen("customer", null, { nav.popBackStack() }) { id ->
                    nav.navigate("person/$id") { popUpTo("person/new") { inclusive = true } }
                }
            }
            composable("person/{id}") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                FriendDetailScreen(id, { nav.popBackStack() }, { nav.navigate("person/$id/profile") }, { nav.navigate("person/$id/transfer") })
            }
            composable("person/{id}/profile") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyProfileScreen("customer", id, { nav.popBackStack() }, { nav.navigate("person/$id/edit") })
            }
            composable("person/{id}/edit") { back ->
                val id = back.arguments?.getString("id").orEmpty()
                PartyFormScreen("customer", id, { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("person/{id}/transfer") { back ->
                TransferFormScreen(back.arguments?.getString("id").orEmpty(), { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("spend") {
                PersonalSpendScreen({ nav.navigate("transaction/$it") }, { nav.navigate("expense/new") })
            }
            composable("expense/new") { ExpenseFormScreen({ nav.popBackStack() }, { nav.popBackStack() }, personalOnly = true) }
            composable("transaction/{id}") { back ->
                TransactionDetailScreen(back.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("notifications") { NotificationsScreen({ nav.popBackStack() }, embedded = true) }
            composable("more") {
                PersonalMoreScreen({ nav.navigate("notifications") })
            }
        }
    }
}

@Composable
fun AuthNav(ui: SessionUi, vm: SessionViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen(ui.busy, ui.error, vm::login) { nav.navigate("register") }
        }
        composable("register") {
            RegisterScreen(ui.busy, ui.error, { nav.popBackStack() }, vm::register)
        }
    }
}

@Composable
fun LockedNav(ui: SessionUi, vm: SessionViewModel) {
    val account = ui.account ?: return
    val nav = rememberNavController()
    NavHost(nav, startDestination = "locked") {
        composable("locked") {
            LockedScreen(
                account = account,
                busy = ui.busy,
                error = ui.error,
                onRefresh = vm::refreshAccount,
                onBusiness = { nav.navigate("business") },
                onNotifications = { nav.navigate("notifications") },
                onLogout = vm::logout,
            )
        }
        composable("business") { BusinessScreen({ nav.popBackStack() }, embedded = false) }
        composable("notifications") { NotificationsScreen({ nav.popBackStack() }, embedded = false) }
    }
}
