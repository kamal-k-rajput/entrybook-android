package com.greenworldlighting.entrybook.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<ResponseBody>

    @PATCH("auth/usage")
    suspend fun setUsage(@Body body: UsageRequest): Response<ResponseBody>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ResponseBody>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ResponseBody>

    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("auth/me")
    suspend fun me(): Response<ResponseBody>

    @GET("business")
    suspend fun business(): Response<ResponseBody>

    @PATCH("business")
    suspend fun updateBusiness(@Body body: BusinessUpdate): Response<ResponseBody>

    @GET("subscription")
    suspend fun subscription(): Response<ResponseBody>

    @GET("notifications")
    suspend fun notifications(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("unread") unread: Boolean?,
    ): Response<ResponseBody>

    @PATCH("notifications/read-all")
    suspend fun readAllNotifications(): Response<ResponseBody>

    @PATCH("notifications/{id}/read")
    suspend fun readNotification(@Path("id") id: String): Response<ResponseBody>

    @GET("customers")
    suspend fun customers(
        @Query("q") q: String?,
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("net") net: String? = null,
    ): Response<ResponseBody>

    @POST("customers")
    suspend fun createCustomer(@Body body: PartyRequest): Response<ResponseBody>

    @GET("customers/{id}")
    suspend fun customer(@Path("id") id: String): Response<ResponseBody>

    @PATCH("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: String, @Body body: PartyRequest): Response<ResponseBody>

    @POST("customers/{id}/status")
    suspend fun customerStatus(@Path("id") id: String, @Body body: StatusRequest): Response<ResponseBody>

    @POST("customers/{id}/sales")
    suspend fun createSale(
        @Path("id") id: String,
        @Body body: SaleRequest,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @POST("customers/{id}/receipts")
    suspend fun createReceipt(
        @Path("id") id: String,
        @Body body: ReceiptRequest,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @GET("vendors")
    suspend fun vendors(
        @Query("q") q: String?,
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("net") net: String? = null,
    ): Response<ResponseBody>

    @POST("vendors")
    suspend fun createVendor(@Body body: PartyRequest): Response<ResponseBody>

    @GET("vendors/{id}")
    suspend fun vendor(@Path("id") id: String): Response<ResponseBody>

    @PATCH("vendors/{id}")
    suspend fun updateVendor(@Path("id") id: String, @Body body: PartyRequest): Response<ResponseBody>

    @POST("vendors/{id}/status")
    suspend fun vendorStatus(@Path("id") id: String, @Body body: StatusRequest): Response<ResponseBody>

    @GET("customers/{id}/transfers")
    suspend fun transfers(
        @Path("id") id: String,
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @POST("customers/{id}/transfers")
    suspend fun transfer(
        @Path("id") id: String,
        @Body body: TransferRequest,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @POST("vendors/{id}/settle")
    suspend fun settleVendor(
        @Path("id") id: String,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @POST("customers/{id}/settle")
    suspend fun settleCustomer(
        @Path("id") id: String,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @POST("customers/{id}/settle-personal")
    suspend fun settlePerson(
        @Path("id") id: String,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @POST("vendors/{id}/payments")
    suspend fun payVendor(
        @Path("id") id: String,
        @Body body: VendorPaymentRequest,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @GET("sales/{id}")
    suspend fun sale(@Path("id") id: String): Response<ResponseBody>

    @PATCH("sales/{id}")
    suspend fun editSale(@Path("id") id: String, @Body body: AmountUpdate): Response<ResponseBody>

    @POST("sales/{id}/void")
    suspend fun voidSale(@Path("id") id: String, @Body body: ReasonRequest): Response<ResponseBody>

    @GET("categories")
    suspend fun categories(
        @Query("expenseType") expenseType: String?,
        @Query("active") active: Boolean?,
    ): Response<ResponseBody>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryRequest): Response<ResponseBody>

    @PATCH("categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryUpdate): Response<ResponseBody>

    @GET("expenses")
    suspend fun expenses(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("expenseType") expenseType: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @POST("expenses")
    suspend fun createExpense(
        @Body body: ExpenseRequest,
        @Header("Idempotency-Key") key: String,
    ): Response<ResponseBody>

    @GET("transactions")
    suspend fun transactions(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("type") type: String?,
        @Query("status") status: String?,
        @Query("paymentMethod") paymentMethod: String?,
        @Query("q") q: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @GET("transactions/{id}")
    suspend fun transaction(@Path("id") id: String): Response<ResponseBody>

    @PATCH("transactions/{id}")
    suspend fun editTransaction(@Path("id") id: String, @Body body: AmountUpdate): Response<ResponseBody>

    @POST("transactions/{id}/reverse")
    suspend fun reverse(@Path("id") id: String, @Body body: ReasonRequest): Response<ResponseBody>

    @GET("dashboard")
    suspend fun dashboard(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/cash-movement")
    suspend fun cashMovement(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/vendors")
    suspend fun vendorReport(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/customers")
    suspend fun customerReport(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/dues")
    suspend fun dues(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/expenses")
    suspend fun expenseReport(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/vendors/{id}/ledger")
    suspend fun vendorLedger(
        @Path("id") id: String,
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @GET("reports/customers/{id}/ledger")
    suspend fun customerLedger(
        @Path("id") id: String,
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @GET("reports/customers/{id}/statement")
    suspend fun statement(
        @Path("id") id: String,
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<ResponseBody>

    @GET("reports/transactions/export")
    suspend fun exportTransactions(
        @Query("period") period: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("type") type: String?,
        @Query("status") status: String?,
        @Query("paymentMethod") paymentMethod: String?,
        @Query("q") q: String?,
    ): Response<ResponseBody>

    @GET("audit-logs")
    suspend fun audit(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>
}
