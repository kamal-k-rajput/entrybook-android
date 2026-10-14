package com.greenworldlighting.entrybook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenworldlighting.entrybook.core.formatDay
import com.greenworldlighting.entrybook.core.subscriptionLabel
import com.greenworldlighting.entrybook.ui.theme.BrandGold
import com.greenworldlighting.entrybook.ui.theme.BrandGreen
import com.greenworldlighting.entrybook.ui.theme.BrandPaper

@Composable
fun SplashScreen(error: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(BrandGreen).systemBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("GWL ENTERPRISES", color = BrandGold, letterSpacing = 1.sp, fontSize = 11.sp)
        Text("Entrybook", color = BrandPaper, fontSize = 26.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(24.dp))
        if (error == null) {
            CircularProgressIndicator(color = BrandGold)
        } else {
            Text(error, color = BrandPaper)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
fun LoginScreen(busy: Boolean, error: String?, onLogin: (String, String, String) -> Unit, onRegister: () -> Unit) {
    val repo = LocalRepo.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf(repo.store.baseUrl.trimEnd('/')) }
    var showServer by rememberSaveable { mutableStateOf(false) }
    AuthFrame(title = "Sign in", subtitle = "Personal money, or the shop book.") {
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        TextButton(onClick = { showServer = !showServer }) { Text(if (showServer) "Hide server" else "Server address") }
        if (showServer) {
            OutlinedTextField(server, { server = it }, label = { Text("API base") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Muted("Emulator: http://10.0.2.2:3000/api/v1. A release build needs https.")
        }
        ErrorLine(error)
        PrimaryButton("Sign in", enabled = !busy) { onLogin(email, password, server) }
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Create an account") }
        Muted("Personal use is the usual choice. Administrators use the website.")
    }
}

@Composable
fun RegisterScreen(busy: Boolean, error: String?, onBack: () -> Unit, onRegister: (String, String, String, String, String, String, String) -> Unit) {
    val repo = LocalRepo.current
    var name by rememberSaveable { mutableStateOf("") }
    var business by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var usage by rememberSaveable { mutableStateOf("personal") }
    var server by rememberSaveable { mutableStateOf(repo.store.baseUrl.trimEnd('/')) }
    AuthFrame(
        title = "Open a book",
        subtitle = if (usage == "personal") "Personal use includes one year." else "A new shop includes six months.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.FilterChip(usage == "personal", { usage = "personal" }, label = { Text("Personal") })
            androidx.compose.material3.FilterChip(usage == "shop", { usage = "shop" }, label = { Text("Shop owner") })
        }
        OutlinedTextField(name, { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (usage == "shop") {
            OutlinedTextField(business, { business = it }, label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), supportingText = { Text("At least 6 characters") })
        OutlinedTextField(server, { server = it }, label = { Text("API base") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        ErrorLine(error)
        PrimaryButton("Create account", enabled = !busy) { onRegister(name, email, phone, password, business, usage, server) }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to sign in") }
    }
}

@Composable
private fun AuthFrame(title: String, subtitle: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(BrandGreen).systemBarsPadding()) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            Text("GWL ENTERPRISES", color = BrandGold, letterSpacing = 1.sp, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("Entrybook", color = BrandPaper, fontSize = 22.sp, fontFamily = FontFamily.Serif)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontSize = 18.sp, fontFamily = FontFamily.Serif)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun LockedScreen(account: Account, busy: Boolean, error: String?, onRefresh: () -> Unit, onBusiness: () -> Unit, onNotifications: () -> Unit, onLogout: () -> Unit) {
    val subscription = account.subscription
    BookScreen(title = account.business?.name ?: "Entrybook", embedded = false, actions = {
        TextButton(onClick = onLogout) { Text("Sign out", color = BrandPaper) }
    }) { padding ->
        FormColumn(padding) {
            Text("Subscription", style = MaterialTheme.typography.headlineMedium)
            Text(lockedCopy(subscription?.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(subscriptionLabel(subscription?.status ?: "pending"), active = subscription?.status == "active")
            if (subscription?.planName != null) DetailRow("Plan", subscription.planName)
            if (subscription?.startDate != null) DetailRow("Starts", formatDay(subscription.startDate))
            if (subscription?.endDate != null) DetailRow("Ends", formatDay(subscription.endDate))
            if (subscription?.amountPaid != null) DetailRow("Amount paid", subscription.amountPaid.shown())
            if (!subscription?.paymentReference.isNullOrBlank()) DetailRow("Payment reference", subscription.paymentReference)
            if (!subscription?.note.isNullOrBlank()) DetailRow("Note", subscription.note)
            ErrorLine(error)
            PrimaryButton(if (busy) "Checking…" else "Check again", enabled = !busy, onClick = onRefresh)
            TextButton(onClick = onBusiness) { Text("Business profile") }
            TextButton(onClick = onNotifications) { Text("Notifications") }
            Muted("You can still update the shop profile and read notifications. The ledger opens after an administrator activates the subscription.")
        }
    }
}

private fun lockedCopy(status: String?): String = when (status) {
    "expired" -> "Your subscription has ended. An administrator can set a new end date."
    "suspended" -> "Your subscription is suspended. Contact Green World Lighting to restore access."
    "active" -> "Checking whether the subscription is still active."
    else -> "Your subscription is waiting. After payment, an administrator turns it on and sets the end date."
}
