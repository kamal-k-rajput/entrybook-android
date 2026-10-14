package com.greenworldlighting.entrybook.data

import com.greenworldlighting.entrybook.core.blankToNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class EntrybookRepository(
    private val api: ApiClient,
    val store: SessionStore,
) {
    var onUnauthorized: (() -> Unit)? = null
    var onSubscriptionRequired: ((Subscription?) -> Unit)? = null

    suspend fun setBaseUrl(raw: String) {
        store.setBaseUrl(raw)
        api.invalidate()
    }

    suspend fun login(email: String, password: String): Session {
        val session = call<Session>(notifyAuth = false) { api.service().login(LoginRequest(email, password)) }
        store.save(session)
        return session
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        businessName: String,
        usage: String,
    ): Session {
        val session = call<Session>(notifyAuth = false) {
            api.service().register(
                RegisterRequest(
                    email = email,
                    password = password,
                    name = name,
                    phone = phone.blankToNull(),
                    businessName = businessName.blankToNull(),
                    usage = usage,
                ),
            )
        }
        store.save(session)
        return session
    }

    suspend fun refresh(): Session {
        val token = store.refreshToken ?: throw ApiException(401, "UNAUTHORIZED", "Sign in again.")
        val session = call<Session>(notifyAuth = false) { api.service().refresh(RefreshRequest(token)) }
        store.save(session)
        return session
    }

    suspend fun logout() {
        try {
            if (store.accessToken != null) {
                call<LoggedOut>(notifyAuth = false) { api.service().logout() }
            }
        } catch (_: Exception) {
        } finally {
            store.clear()
        }
    }

    suspend fun me(notifyAuth: Boolean = true): Me = call(notifyAuth) { api.service().me() }

    suspend fun setUsage(usage: String): Me = call { api.service().setUsage(UsageRequest(usage)) }

    suspend fun business(): BusinessProfile = call { api.service().business() }

    suspend fun updateBusiness(name: String, phone: String, address: String): BusinessProfile =
        call {
            api.service().updateBusiness(
                BusinessUpdate(name = name.trim(), phone = phone.trim(), address = address.trim()),
            )
        }

    suspend fun subscription(): Subscription? = callNullable { api.service().subscription() }

    suspend fun notifications(page: Int, unread: Boolean? = null): Page<Notice> =
        call { api.service().notifications(page, 30, unread) }

    suspend fun markRead(id: String): Notice = call { api.service().readNotification(id) }

    suspend fun markAllRead() {
        call<UpdatedFlag> { api.service().readAllNotifications() }
    }

    suspend fun parties(kind: String, query: String?, status: String?, page: Int, net: String? = null): Page<Party> {
        val q = query.blankToNull()
        return if (kind == "vendor") {
            call { api.service().vendors(q, status, page, 20, net) }
        } else {
            call { api.service().customers(q, status, page, 20, net) }
        }
    }

    suspend fun party(kind: String, id: String): Party =
        if (kind == "vendor") call { api.service().vendor(id) } else call { api.service().customer(id) }

    suspend fun saveParty(kind: String, id: String?, body: PartyRequest): Party = when {
        kind == "vendor" && id == null -> call { api.service().createVendor(body) }
        kind == "vendor" -> call { api.service().updateVendor(id!!, body) }
        id == null -> call { api.service().createCustomer(body) }
        else -> call { api.service().updateCustomer(id, body) }
    }

    suspend fun setPartyStatus(kind: String, id: String, status: String): Party {
        val body = StatusRequest(status)
        return if (kind == "vendor") {
            call { api.service().vendorStatus(id, body) }
        } else {
            call { api.service().customerStatus(id, body) }
        }
    }

    suspend fun createSale(customerId: String, body: SaleRequest, key: String): SaleCreated =
        call { api.service().createSale(customerId, body, key) }

    suspend fun createReceipt(customerId: String, body: ReceiptRequest, key: String): ReceiptResult =
        call { api.service().createReceipt(customerId, body, key) }

    suspend fun sale(id: String): Sale = call { api.service().sale(id) }

    suspend fun voidSale(id: String, reason: String): Sale =
        call { api.service().voidSale(id, ReasonRequest(reason.trim())) }

    suspend fun payVendor(vendorId: String, body: VendorPaymentRequest, key: String): VendorPaymentResult =
        call { api.service().payVendor(vendorId, body, key) }

    suspend fun settleVendor(id: String, key: String) {
        call<SettleResult> { api.service().settleVendor(id, key) }
    }

    suspend fun settleCustomer(id: String, key: String) {
        call<SettleResult> { api.service().settleCustomer(id, key) }
    }

    suspend fun settlePerson(id: String, key: String) {
        call<SettleResult> { api.service().settlePerson(id, key) }
    }

    suspend fun categories(expenseType: String? = null, active: Boolean? = null): List<Category> =
        call { api.service().categories(expenseType, active) }

    suspend fun createCategory(name: String, expenseType: String): Category =
        call { api.service().createCategory(CategoryRequest(name.trim(), expenseType)) }

    suspend fun updateCategory(id: String, name: String? = null, active: Boolean? = null): Category =
        call { api.service().updateCategory(id, CategoryUpdate(name?.trim(), active)) }

    suspend fun createExpense(body: ExpenseRequest, key: String): Transaction =
        call { api.service().createExpense(body, key) }

    suspend fun transactions(
        period: String,
        from: String?,
        to: String?,
        type: String?,
        status: String?,
        paymentMethod: String?,
        query: String?,
        page: Int,
        limit: Int = 20,
    ): TransactionPage = call {
        api.service().transactions(period, from, to, type.blankToNull(), status.blankToNull(), paymentMethod.blankToNull(), query.blankToNull(), page, limit)
    }

    suspend fun transaction(id: String): Transaction = call { api.service().transaction(id) }

    suspend fun reverse(id: String, reason: String): Transaction =
        call { api.service().reverse(id, ReasonRequest(reason.trim())) }

    suspend fun dashboard(period: String, from: String?, to: String?): Dashboard =
        call { api.service().dashboard(period, from, to) }

    suspend fun cashMovement(period: String, from: String?, to: String?): CashMovement =
        call { api.service().cashMovement(period, from, to) }

    suspend fun vendorReport(period: String, from: String?, to: String?): VendorReport =
        call { api.service().vendorReport(period, from, to) }

    suspend fun customerReport(period: String, from: String?, to: String?): CustomerReport =
        call { api.service().customerReport(period, from, to) }

    suspend fun dues(period: String, from: String?, to: String?): DuesReport =
        call { api.service().dues(period, from, to) }

    suspend fun expenseReport(period: String, from: String?, to: String?): ExpenseReport =
        call { api.service().expenseReport(period, from, to) }

    suspend fun vendorLedger(id: String, period: String, from: String?, to: String?, page: Int = 1, limit: Int = 20): VendorLedger =
        call { api.service().vendorLedger(id, period, from, to, page, limit) }

    suspend fun transfers(id: String, period: String, from: String?, to: String?, page: Int, limit: Int): FriendLedger =
        call { api.service().transfers(id, period, from, to, page, limit) }

    suspend fun transfer(id: String, body: TransferRequest, key: String): Transaction =
        call { api.service().transfer(id, body, key) }

    suspend fun customerLedger(id: String, period: String, from: String?, to: String?, page: Int = 1, limit: Int = 20): CustomerLedger =
        call { api.service().customerLedger(id, period, from, to, page, limit) }

    suspend fun editSale(id: String, amount: String): Sale =
        call { api.service().editSale(id, AmountUpdate(amount)) }

    suspend fun editTransaction(id: String, amount: String): Transaction =
        call { api.service().editTransaction(id, AmountUpdate(amount)) }

    suspend fun statement(id: String, period: String, from: String?, to: String?): CustomerLedger =
        call { api.service().statement(id, period, from, to) }

    suspend fun exportTransactions(
        period: String,
        from: String?,
        to: String?,
        type: String?,
        status: String?,
        query: String?,
    ): ByteArray {
        val response = try {
            api.service().exportTransactions(period, from, to, type.blankToNull(), status.blankToNull(), null, query.blankToNull())
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Can't reach the server. Check the address and your connection.")
        }
        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string().orEmpty()
            val error = parseError(response.code(), raw)
            if (error.code == "SUBSCRIPTION_REQUIRED") onSubscriptionRequired?.invoke(error.subscription)
            if (error.statusCode == 401) onUnauthorized?.invoke()
            throw error
        }
        return response.body()?.bytes() ?: byteArrayOf()
    }

    suspend fun audit(page: Int): Page<AuditEntry> = call { api.service().audit(page, 30) }

    private suspend inline fun <reified T> call(
        notifyAuth: Boolean = true,
        block: suspend () -> Response<ResponseBody>,
    ): T {
        val data = execute<T>(notifyAuth, block)
        return data ?: throw ApiException(200, "EMPTY", "The server returned an empty response.")
    }

    private suspend inline fun <reified T> callNullable(
        notifyAuth: Boolean = true,
        block: suspend () -> Response<ResponseBody>,
    ): T? = execute(notifyAuth, block)

    private suspend inline fun <reified T> execute(
        notifyAuth: Boolean,
        block: suspend () -> Response<ResponseBody>,
    ): T? {
        val response = try {
            block()
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Can't reach the server. Check the address and your connection.")
        }
        val raw = if (response.isSuccessful) {
            response.body()?.string().orEmpty()
        } else {
            response.errorBody()?.string().orEmpty()
        }
        if (!response.isSuccessful) {
            val error = parseError(response.code(), raw)
            if (error.code == "SUBSCRIPTION_REQUIRED") onSubscriptionRequired?.invoke(error.subscription)
            if (notifyAuth && error.statusCode == 401) onUnauthorized?.invoke()
            throw error
        }
        return try {
            val envelope = api.json.decodeFromString(Envelope.serializer(serializer<T>()), raw)
            if (!envelope.success) {
                throw ApiException(response.code(), "BAD_RESPONSE", "The server rejected the request.")
            }
            envelope.data
        } catch (e: ApiException) {
            throw e
        } catch (_: SerializationException) {
            throw ApiException(response.code(), "PARSE", "Couldn't read the server response.")
        }
    }

    private fun parseError(status: Int, raw: String): ApiException {
        if (raw.isBlank()) return ApiException(status, codeFor(status), fallback(status))
        return try {
            val element = api.json.parseToJsonElement(raw)
            if (element !is JsonObject) return ApiException(status, codeFor(status), fallback(status))
            val message = when (val value = element["message"]) {
                is JsonArray -> value.mapNotNull { item ->
                    (item as? JsonPrimitive)?.contentOrNull
                }.joinToString("\n")
                is JsonPrimitive -> value.content
                else -> fallback(status)
            }
            val code = (element["code"] as? JsonPrimitive)?.content ?: codeFor(status)
            val subscription = element["subscription"]?.let { value ->
                if (value is JsonNull) null
                else api.json.decodeFromJsonElement(Subscription.serializer(), value)
            }
            ApiException(status, code, message.ifBlank { fallback(status) }, subscription)
        } catch (_: Exception) {
            ApiException(status, codeFor(status), fallback(status))
        }
    }

    private fun codeFor(status: Int): String = when (status) {
        400 -> "BAD_REQUEST"
        401 -> "UNAUTHORIZED"
        403 -> "FORBIDDEN"
        404 -> "NOT_FOUND"
        409 -> "CONFLICT"
        429 -> "TOO_MANY_REQUESTS"
        0 -> "NETWORK"
        else -> "INTERNAL_ERROR"
    }

    private fun fallback(status: Int): String = when (status) {
        0 -> "Can't reach the server. Check the address and your connection."
        401 -> "Sign in again."
        403 -> "You don't have access to this."
        404 -> "That record was not found."
        409 -> "This conflicts with something already saved."
        429 -> "Too many requests. Try again shortly."
        else -> "Something went wrong."
    }
}

private fun String?.blankToNull(): String? = this?.trim()?.ifBlank { null }
