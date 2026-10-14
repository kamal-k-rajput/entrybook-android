package com.greenworldlighting.entrybook.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

const val DEFAULT_BASE_URL = "https://entrybook.greenworldlighting.com/api/v1/"

private val KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
private val DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(KOLKATA)

val PERIODS = listOf(
    "today" to "Today",
    "this_week" to "This week",
    "this_month" to "This month",
    "previous_month" to "Last month",
    "custom" to "Custom",
)

val PAYMENT_METHODS = listOf(
    "cash" to "Cash",
    "upi" to "UPI",
    "bank_transfer" to "Bank transfer",
    "card" to "Card",
    "cheque" to "Cheque",
    "other" to "Other",
)

val TX_TYPES = listOf(
    "" to "All types",
    "customer_receipt" to "Receipts",
    "vendor_payment" to "Vendor payments",
    "business_expense" to "Business expenses",
    "personal_expense" to "Personal expenses",
)

fun todayKolkata(): String = LocalDate.now(KOLKATA).toString()

fun formatDay(day: String?): String {
    if (day.isNullOrBlank()) return "—"
    return try {
        LocalDate.parse(day).format(DAY)
    } catch (_: Exception) {
        day
    }
}

fun formatWhen(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        WHEN.format(Instant.parse(iso))
    } catch (_: Exception) {
        iso
    }
}

fun millisToDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

fun dateToMillis(date: String): Long? = try {
    LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (_: Exception) {
    null
}

fun periodReady(period: String, from: String, to: String): Boolean =
    period != "custom" || (from.isNotBlank() && to.isNotBlank() && from <= to)

fun labelOf(options: List<Pair<String, String>>, value: String): String =
    options.firstOrNull { it.first == value }?.second ?: value.replace('_', ' ')

fun paymentLabel(method: String): String = labelOf(PAYMENT_METHODS, method)

fun typeLabel(type: String): String = when (type) {
    "customer_receipt" -> "Receipt"
    "vendor_payment" -> "Vendor payment"
    "personal_expense" -> "Personal expense"
    "business_expense" -> "Business expense"
    "personal_given" -> "Given"
    "personal_received" -> "Received"
    "vendor_settlement" -> "Settled"
    "sale" -> "Sale"
    "receipt" -> "Receipt"
    else -> labelOf(TX_TYPES, type)
}

fun saleStatusLabel(status: String): String = when (status) {
    "open" -> "Open"
    "partial" -> "Partial"
    "paid" -> "Paid"
    "void" -> "Void"
    else -> status
}

fun subscriptionLabel(status: String): String = when (status) {
    "pending" -> "Pending"
    "active" -> "Active"
    "expired" -> "Expired"
    "suspended" -> "Suspended"
    else -> status
}

fun bucketLabel(bucket: String): String = when (bucket) {
    "no_due_date" -> "No due date"
    "current" -> "Not due"
    "1_30" -> "1–30 days"
    "31_60" -> "31–60 days"
    "61_90" -> "61–90 days"
    "over_90" -> "Over 90 days"
    else -> bucket
}

fun normalizeBaseUrl(raw: String): String {
    var url = raw.trim().trimEnd('/')
    if (url.isBlank()) throw IllegalArgumentException("Enter the server address")
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://$url"
    }
    if (!url.endsWith("/api/v1")) url += "/api/v1"
    return "$url/"
}

fun normalizeRupees(raw: String, allowZero: Boolean = false): String {
    val trimmed = raw.trim()
    if (!Regex("""^\d+(\.\d{1,2})?$""").matches(trimmed)) {
        throw IllegalArgumentException("Enter an amount like 1200.50")
    }
    val parts = trimmed.split('.')
    val whole = parts[0].trimStart('0').ifEmpty { "0" }
    if (whole.length > 11) throw IllegalArgumentException("Amount is too large")
    val fraction = (parts.getOrNull(1) ?: "").padEnd(2, '0')
    val paise = whole.toLong() * 100 + fraction.toLong()
    if (paise > 100_000_000_000L) throw IllegalArgumentException("Amount is too large")
    if (paise == 0L && !allowZero) throw IllegalArgumentException("Amount must be greater than zero")
    return "$whole.$fraction"
}

fun collectLabel(paise: Long): String {
    val amount = paiseDisplay(kotlin.math.abs(paise))
    return if (paise < 0) "Advance $amount" else "Collect $amount"
}

fun paiseDisplay(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val abs = kotlin.math.abs(paise)
    return "$sign₹${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}

fun rupeePaise(raw: String, allowZero: Boolean = false): Long {
    val normalized = normalizeRupees(raw, allowZero)
    val parts = normalized.split('.')
    return parts[0].toLong() * 100 + parts[1].toLong()
}

fun emailError(email: String): String? {
    val value = email.trim()
    if (value.isEmpty()) return "Enter an email"
    if (!value.contains('@') || value.startsWith('@') || value.endsWith('@') || value.contains(' ')) {
        return "Enter a valid email"
    }
    return null
}

fun passwordError(password: String): String? = when {
    password.length < 6 -> "Use at least 6 characters"
    else -> null
}

fun reasonError(reason: String): String? = when {
    reason.trim().length < 3 -> "Enter at least 3 characters"
    reason.trim().length > 300 -> "Use at most 300 characters"
    else -> null
}

fun newIdempotencyKey(): String = UUID.randomUUID().toString()

fun String.blankToNull(): String? = trim().ifBlank { null }
