package com.budgettracker.domain.usecase

import com.budgettracker.domain.model.BankRegistry
import com.budgettracker.domain.model.SmsMessage
import com.budgettracker.domain.model.Transaction
import com.budgettracker.domain.model.TransactionType

class BankMessageParser {

    fun parseBankSms(sms: SmsMessage): Transaction? {
        val bankName = BankRegistry.identifyBank(sms.address, sms.body) ?: return null
        val amount = extractAmount(sms.body) ?: return null
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
            category = category
        )
    }

    fun parseMultiple(smsList: List<SmsMessage>): List<Transaction> {
        return smsList.mapNotNull { parseBankSms(it) }
    }

    private fun detectTransactionType(body: String): TransactionType {
        val lower = body.lowercase()

        val creditPatterns = listOf(
            "\\bcredited\\b", "\\bcredit\\b", "\\bdeposited\\b", "\\breceived\\b",
            "\\badded\\b", "\\bdeposit\\b"
        ).map { it.toRegex(RegexOption.IGNORE_CASE) }

        val debitPatterns = listOf(
            "\\bdebited\\b", "\\bdebit\\b", "\\bwithdrawn\\b", "\\bpaid\\b",
            "\\bdeducted\\b", "\\bspent\\b", "\\btransfer\\b", "\\bmandate\\b"
        ).map { it.toRegex(RegexOption.IGNORE_CASE) }

        val creditScore = creditPatterns.count { it.containsMatchIn(lower) }
        val debitScore = debitPatterns.count { it.containsMatchIn(lower) }

        return if (creditScore > debitScore) TransactionType.CREDIT else TransactionType.DEBIT
    }

    private fun extractAmount(body: String): Double? {
        val patterns = listOf(
            """(?:INR|Rs\.?|Rs)\s*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE),
            """([\d,]+\.\d{2})\s*(?:INR|Rs)""".toRegex(RegexOption.IGNORE_CASE),
            """(?:Rs\.?\s*)?([\d,]+\.\d{2})""".toRegex(RegexOption.IGNORE_CASE),
            """debit(?:ed)?\s*(?:INR|Rs\.?)?\s*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE),
            """credit(?:ed)?(?:by)?\s*(?:INR|Rs\.?)?\s*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amountStr = match.groupValues[1]
                    .replace(",", "")
                    .replace(" ", "")
                    .trim()

                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        return null
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
}