package com.greenworldlighting.entrybook.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Envelope<T>(
    val success: Boolean = false,
    val data: T? = null,
)

@Serializable
data class Money(
    val paise: Long = 0,
    val rupees: String = "0.00",
    val display: String = "₹0.00",
)

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val businessId: String? = null,
    val usage: String = "shop",
)

@Serializable
data class BusinessProfile(
    val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val currency: String = "INR",
    val timezone: String = "Asia/Kolkata",
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Subscription(
    val id: String,
    val businessId: String,
    val status: String,
    val planName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val paymentReference: String? = null,
    val amountPaid: Money? = null,
    val note: String? = null,
    val active: Boolean = false,
    val activatedAt: String? = null,
)

@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int = 900,
    val user: UserProfile,
    val subscription: Subscription? = null,
)

@Serializable
data class Me(
    val user: UserProfile,
    val business: BusinessProfile? = null,
    val subscription: Subscription? = null,
)

@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val given: Money? = null,
    val received: Money? = null,
    val paid: Money? = null,
)

@Serializable
data class Notice(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val readAt: String? = null,
    val createdAt: String,
)

@Serializable
data class Party(
    val id: String,
    val type: String,
    val name: String,
    val phone: String? = null,
    val businessName: String? = null,
    val address: String? = null,
    val code: String? = null,
    val notes: String? = null,
    val status: String,
    val openingBalance: Money,
    val given: Money? = null,
    val received: Money? = null,
    val outstanding: Money? = null,
    val paid: Money? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Transaction(
    val id: String,
    val businessId: String? = null,
    val partyId: String? = null,
    val partyName: String? = null,
    val categoryId: String? = null,
    val type: String,
    val direction: String,
    val date: String,
    val amount: Money,
    val paymentMethod: String,
    val reference: String? = null,
    val notes: String? = null,
    val status: String,
    val reversalReason: String? = null,
    val reversedAt: String? = null,
    val createdAt: String? = null,
    val edited: Boolean = false,
    val history: List<AmountRevision> = emptyList(),
)

@Serializable
data class TransactionPage(
    val period: PeriodRange? = null,
    val items: List<Transaction> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
)

@Serializable
data class Sale(
    val id: String,
    val customerId: String,
    val invoiceNumber: String? = null,
    val date: String,
    val grossAmount: Money,
    val discount: Money,
    val netAmount: Money,
    val dueDate: String? = null,
    val status: String,
    val notes: String? = null,
    val voidReason: String? = null,
    val createdAt: String? = null,
    val allocated: Money? = null,
    val remaining: Money? = null,
    val edited: Boolean = false,
    val history: List<AmountRevision> = emptyList(),
)

@Serializable
data class AmountRevision(
    val amount: Money,
    val at: String,
)

@Serializable
data class AmountUpdate(val amount: String)

@Serializable
data class ReceiptResult(
    val receipt: Transaction,
    val outstanding: Money,
)

@Serializable
data class SaleCreated(
    val sale: Sale,
    val receipt: ReceiptResult? = null,
)

@Serializable
data class VendorPaymentResult(
    val transaction: Transaction,
    val vendor: VendorBalance,
)

@Serializable
data class VendorBalance(
    val id: String,
    val name: String,
    val paidTillDate: Money,
    val payable: Money,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val expenseType: String,
    val active: Boolean = true,
    val isSystem: Boolean = false,
)

@Serializable
data class PeriodRange(
    val from: String,
    val to: String,
    val period: String,
)

@Serializable
data class CashBlock(
    val inflow: Money,
    val outflow: Money,
    val net: Money,
    val personalExpense: Money,
    val businessOutflow: Money,
    val note: String? = null,
)

@Serializable
data class Dashboard(
    val period: PeriodRange,
    val currency: String = "INR",
    val cash: CashBlock,
    val sales: SalesBlock,
    val customers: OutstandingBlock,
    val vendors: VendorDashBlock,
    val personal: PersonalBlock = PersonalBlock(),
)

@Serializable
data class SalesBlock(
    val invoicedInPeriod: Money,
    val note: String? = null,
)

@Serializable
data class OutstandingBlock(val outstanding: Money)

@Serializable
data class PersonalBlock(
    val given: Money = Money(),
    val received: Money = Money(),
    val spent: Money = Money(),
)

@Serializable
data class VendorDashBlock(
    val paidInPeriod: Money,
    val payable: Money,
    val note: String? = null,
)

@Serializable
data class CashMovement(
    val period: PeriodRange,
    val inflow: Money,
    val outflow: Money,
    val net: Money,
    val personalExpense: Money,
    val businessOutflow: Money,
    val note: String? = null,
)

@Serializable
data class VendorReport(
    val period: PeriodRange,
    val items: List<VendorReportItem> = emptyList(),
)

@Serializable
data class VendorReportItem(
    val vendor: Party,
    val paidInPeriod: Money,
    val paidTillDate: Money,
    val payable: Money,
)

@Serializable
data class CustomerReport(
    val period: PeriodRange,
    val items: List<CustomerReportItem> = emptyList(),
)

@Serializable
data class CustomerReportItem(
    val customer: Party,
    val salesInPeriod: Money,
    val receivedInPeriod: Money,
    val outstanding: Money,
)

@Serializable
data class DuesReport(
    val asOf: String,
    val invoices: List<DueInvoice> = emptyList(),
)

@Serializable
data class DueInvoice(
    val saleId: String,
    val customerId: String,
    val customerName: String? = null,
    val invoiceNumber: String? = null,
    val date: String,
    val dueDate: String? = null,
    val remaining: Money,
    val bucket: String,
)

@Serializable
data class ExpenseReport(
    val period: PeriodRange,
    val expenseType: String = "personal",
    val total: Money,
    val items: List<ExpenseReportItem> = emptyList(),
    val note: String? = null,
)

@Serializable
data class ExpenseReportItem(
    val categoryId: String? = null,
    val categoryName: String,
    val total: Money,
)

@Serializable
data class VendorLedger(
    val vendor: Party,
    val period: PeriodRange,
    val summary: VendorLedgerSummary,
    val transactions: List<LedgerPayment> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
)

@Serializable
data class VendorLedgerSummary(
    val paidInPeriod: Money,
    val paidTillDate: Money,
    val payable: Money,
    val transactionCount: Int = 0,
)

@Serializable
data class LedgerPayment(
    val id: String,
    val date: String,
    val amount: Money,
    val paymentMethod: String,
    val reference: String? = null,
    val notes: String? = null,
    val type: String? = null,
    val status: String,
    val edited: Boolean = false,
)

@Serializable
data class CustomerLedger(
    val customer: Party,
    val period: PeriodRange,
    val generatedAt: String? = null,
    val openingBalance: Money,
    val outstanding: Money,
    val soldInPeriod: Money? = null,
    val receivedInPeriod: Money? = null,
    val entries: List<StatementEntry> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
)

@Serializable
data class StatementEntry(
    val id: String,
    val kind: String,
    val date: String,
    val description: String,
    val amount: Money,
    val balance: Money,
    val edited: Boolean = false,
)

@Serializable
data class AuditEntry(
    val id: String,
    val businessId: String? = null,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val before: JsonElement? = null,
    val after: JsonElement? = null,
    val createdAt: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null,
    val businessName: String? = null,
    val usage: String = "personal",
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class UsageRequest(val usage: String)

@Serializable
data class TransferRequest(
    val direction: String,
    val amount: String,
    val date: String,
    val paymentMethod: String,
    val notes: String? = null,
    val confirmInactive: Boolean? = null,
)

@Serializable
data class FriendLedger(
    val period: PeriodRange,
    val given: Money,
    val received: Money,
    val givenToDate: Money? = null,
    val receivedToDate: Money? = null,
    val items: List<Transaction> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
)

@Serializable
data class BusinessUpdate(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
)

@Serializable
data class PartyRequest(
    val name: String,
    val phone: String? = null,
    val businessName: String? = null,
    val address: String? = null,
    val code: String? = null,
    val notes: String? = null,
    val openingBalance: String? = null,
)

@Serializable
data class StatusRequest(val status: String)

@Serializable
data class InitialReceiptRequest(
    val amount: String,
    val date: String,
    val paymentMethod: String,
    val reference: String? = null,
    val notes: String? = null,
)

@Serializable
data class SaleRequest(
    val date: String,
    val amount: String,
    val discount: String? = null,
    val invoiceNumber: String? = null,
    val dueDate: String? = null,
    val notes: String? = null,
    val confirmInactive: Boolean? = null,
    val allowAdvance: Boolean? = null,
    val initialReceipt: InitialReceiptRequest? = null,
)

@Serializable
data class AllocationRequest(val saleId: String, val amount: String)

@Serializable
data class ReceiptRequest(
    val amount: String,
    val date: String,
    val paymentMethod: String,
    val reference: String? = null,
    val notes: String? = null,
    val allowAdvance: Boolean? = null,
    val confirmInactive: Boolean? = null,
    val allocations: List<AllocationRequest>? = null,
)

@Serializable
data class VendorPaymentRequest(
    val amount: String,
    val date: String,
    val paymentMethod: String,
    val reference: String? = null,
    val notes: String? = null,
    val categoryId: String? = null,
    val confirmInactive: Boolean? = null,
)

@Serializable
data class ExpenseRequest(
    val expenseType: String,
    val amount: String,
    val date: String,
    val categoryId: String,
    val paymentMethod: String,
    val notes: String? = null,
)

@Serializable
data class CategoryRequest(val name: String, val expenseType: String)

@Serializable
data class CategoryUpdate(val name: String? = null, val active: Boolean? = null)

@Serializable
data class ReasonRequest(val reason: String)

@Serializable
data class LoggedOut(val loggedOut: Boolean = true)

@Serializable
data class UpdatedFlag(val updated: Boolean = true)

@Serializable
data class SettleResult(val settled: Boolean = true)
