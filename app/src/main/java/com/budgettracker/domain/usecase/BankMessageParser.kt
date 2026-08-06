package com.budgettracker.domain.usecase

import com.budgettracker.domain.model.BankRegistry
import com.budgettracker.domain.model.SmsMessage
import com.budgettracker.domain.model.Transaction
import com.budgettracker.domain.model.TransactionType
import java.util.Locale

class BankMessageParser {

    fun parseBankSms(sms: SmsMessage): Transaction? {
        val bankName = BankRegistry.identifyBank(sms.address, sms.body) ?: return null
        val amount = extractAmount(sms.body) ?: return null

        // Skip UPI Mandate messages — they are requests, not actual transactions
        val lower = sms.body.lowercase()
        if (lower.contains("mandate")) return null

        val transactionType = detectTransactionType(sms.body)
        val accountLast4 = extractAccountLast4(sms.body)
        val category = detectCategory(sms.body)

        return Transaction(
            id = 0,
            source = "SMS",
            bankName = bankName,
            accountLast4 = accountLast4,
            amount = amount,
            currency = "INR",
            transactionType = transactionType,
            timestamp = sms.date,
            rawMessage = sms.body,
            recipientName = null,
            category = category,
            smsId = sms.id,
            smsThreadId = sms.threadId,
            smsAddress = sms.address,
            smsDate = sms.date,
            transactionFingerprint = buildFingerprint(sms, amount, transactionType, accountLast4)
        )
    }

    fun parseMultiple(smsList: List<SmsMessage>): List<Transaction> {
        return smsList.mapNotNull { parseBankSms(it) }
    }

    private fun detectTransactionType(body: String): TransactionType {
        val lower = body.lowercase()

        val creditPatterns = listOf(
            "\\bcredited\\b", "\\bcredit\\b", "\\bdeposited\\b", "\\breceived\\b",
            "\\badded\\b", "\\bdeposit\\b",
            "\\brefund(?:ed)?\\b", "\\breversal\\b", "\\breversed\\b", "\\bcashback\\b",
            "\\breward\\b", "\\binterest\\b",
            "\\btransfer(?:red)?\\s+from\\s+(?!your|ur\\b)"
        ).map { it.toRegex(RegexOption.IGNORE_CASE) }

        val debitPatterns = listOf(
            "\\bdebited\\b", "\\bdebit\\b", "\\bwithdrawn\\b", "\\bpaid\\b",
            "\\bdeducted\\b", "\\bspent\\b",
            "\\btransfer(?:red)?\\s+to\\b",
            "\\btransfer(?:red)?\\s+from\\s+(?:your|ur\\b)"
        ).map { it.toRegex(RegexOption.IGNORE_CASE) }

        val creditScore = creditPatterns.count { it.containsMatchIn(lower) }
        val debitScore = debitPatterns.count { it.containsMatchIn(lower) }

        return when {
            creditScore > debitScore -> TransactionType.CREDIT
            debitScore > creditScore -> TransactionType.DEBIT
            // Tie (e.g. "transfer received"): credit keywords like received/
            // credited are the more specific signal, so default to CREDIT
            creditScore > 0 -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }
    }

    private fun extractAmount(body: String): Double? {
        val candidates = mutableListOf<Pair<Int, Double>>() // token position -> amount

        // Currency-anchored amounts: Rs / INR / ₹ before or after the number
        Regex("""(?:INR|Rs\.?|Rs|₹)\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .findAll(body)
            .forEach { m ->
                parseAmount(m.groupValues[1])?.let { candidates += m.range.first to it }
            }
        Regex("""([\d,]+\.\d{2})\s*(?:INR|Rs|₹)""", RegexOption.IGNORE_CASE)
            .findAll(body)
            .forEach { m ->
                parseAmount(m.groupValues[1])?.let { candidates += m.range.first to it }
            }

        // Bare two-decimal amounts as a fallback — but never part of a date
        // (12.03.2025) and never an implausibly large reference number (UTR …012.00)
        Regex("""(?<![\d.,])([\d,]+\.\d{2})(?![\d.])""", RegexOption.IGNORE_CASE)
            .findAll(body)
            .forEach { m ->
                val amount = parseAmount(m.groupValues[1])
                if (amount != null && amount < 1_000_000_000) {
                    candidates += m.range.first to amount
                }
            }

        if (candidates.isEmpty()) return null

        // Drop balance readings: "Avl Bal Rs.X" or "Rs.X Avl Bal"
        val balanceMarkers = Regex(
            """\b(?:avl\.?\s*bal(?:ance)?|available\s*balance|avail\s*bal(?:ance)?|bal(?:ance)?)\b""",
            RegexOption.IGNORE_CASE
        ).findAll(body).map { it.range }.toList()
        val filtered = candidates.filter { (pos, _) ->
            balanceMarkers.none { m ->
                (pos >= m.last - 2 && pos <= m.last + 14) || // marker immediately before amount
                    (pos <= m.first && (m.first - pos) <= 8) // amount immediately before marker
            }
        }

        // Prefer the candidate nearest to a transaction keyword
        val keywords = Regex(
            """debited|debit|credited|credit|deposited|withdrawn|received|added|paid|spent|deducted""",
            RegexOption.IGNORE_CASE
        ).findAll(body).map { it.range.first }.toList()

        fun distance(pos: Int): Int =
            if (keywords.isEmpty()) Int.MAX_VALUE else keywords.minOf { kotlin.math.abs(it - pos) }

        val pool = if (filtered.isNotEmpty()) filtered else candidates
        return pool.minByOrNull { (pos, _) -> distance(pos) }?.second
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace(",", "").replace(" ", "").trim()
        val amount = cleaned.toDoubleOrNull()
        return if (amount != null && amount > 0) amount else null
    }

    private fun extractAccountLast4(body: String): String {
        val patterns = listOf(
            """\*+\d{4}""".toRegex(),
            """A/c\s+\*+\d{3,4}""".toRegex(RegexOption.IGNORE_CASE),
            """a/c\s+\*+\d{3,4}""".toRegex(RegexOption.IGNORE_CASE),
            """ac\s+\*+\d{3,4}""".toRegex(RegexOption.IGNORE_CASE),
            """\*\*\d{4}""".toRegex(),
            """\d{4}\s+credited""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                val digits = match.value.replace("*", "").filter { it.isDigit() }
                if (digits.length >= 3) {
                    return digits.takeLast(4).padStart(4, '0')
                }
            }
        }
        return "0000"
    }

    private fun detectCategory(body: String): String? {
        val lower = body.lowercase()

        return when {
            isSalaryCredit(lower) -> "SALARY"

            lower.contains("upi") || lower.contains("google pay") ||
            lower.contains("phonepe") || lower.contains("paytm") -> "UPI"

            lower.contains("atm") || lower.contains("withdrawal") -> "ATM"

            lower.contains("neft") || lower.contains("imps") ||
            lower.contains("rtgs") || lower.contains("transfer") -> "TRANSFER"

            lower.contains("emi") || lower.contains("loan") -> "EMI"

            lower.contains("bill") || lower.contains("electricity") ||
            lower.contains("gas") || lower.contains("water") -> "BILL_PAYMENT"

            lower.contains("shopping") || lower.contains("amazon") ||
            lower.contains("flipkart") || lower.contains("myntra") -> "SHOPPING"

            lower.contains("food") || lower.contains("zomato") ||
            lower.contains("swiggy") || lower.contains("restaurant") -> "FOOD"

            lower.contains("petrol") || lower.contains("fuel") ||
            lower.contains("gas") -> "FUEL"

            lower.contains("medicine") || lower.contains("pharma") ||
            lower.contains("doctor") || lower.contains("hospital") -> "HEALTHCARE"

            lower.contains(" recharge") || lower.contains("mobile") -> "RECHARGE"

            else -> null
        }
    }

    private fun isSalaryCredit(lowerBody: String): Boolean {
        val salaryKeywords = listOf(
            "salary",
            "payroll",
            "monthly pay",
            "wages",
            "stipend",
            "remuneration",
            "employee salary",
            "salary credit",
            "sal cr",
            "sal credited"
        )
        return salaryKeywords.any { lowerBody.contains(it) }
    }

    private fun buildFingerprint(
        sms: SmsMessage,
        amount: Double,
        transactionType: TransactionType,
        accountLast4: String
    ): String {
        val normalizedBody = sms.body
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
        return listOf(
            "sms",
            sms.id,
            sms.threadId,
            sms.date,
            sms.address.lowercase(),
            transactionType.name,
            String.format(Locale.US, "%.2f", amount),
            accountLast4,
            normalizedBody.hashCode()
        ).joinToString("|")
    }
}
