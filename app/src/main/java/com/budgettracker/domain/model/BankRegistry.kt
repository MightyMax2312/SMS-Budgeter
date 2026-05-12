package com.budgettracker.domain.model

object BankRegistry {

    val banks = listOf(
        Bank("HDFC", listOf("hdfcbank", "hdfc")),
        Bank("ICICI", listOf("icicib", "icici")),
        Bank("SBI", listOf("sbiben", "sbi")),
        Bank("Axis Bank", listOf("axisb", "axis")),
        Bank("Kotak", listOf("kotakb", "kotak")),
        Bank("PNB", listOf("pnbbnk", "pnb")),
        Bank("Yes Bank", listOf("yesbank", "yesb")),
        Bank("Bank of Baroda", listOf("bobbk", "bob"))
    )

    fun identifyBank(address: String?, body: String): String? {
        val lowerBody = body.lowercase()
        val lowerAddress = (address ?: "").lowercase()

        return banks.find { bank ->
            bank.senderIds.any { id ->
                lowerBody.contains(id) || lowerAddress.contains(id)
            }
        }?.name
    }
}