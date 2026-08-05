package com.budgettracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankFilterTest {

    private fun tx(bank: String) = Transaction(
        id = 0,
        source = "SMS",
        bankName = bank,
        accountLast4 = "0000",
        amount = 100.0,
        currency = "INR",
        transactionType = TransactionType.DEBIT,
        timestamp = 0L,
        rawMessage = "",
        recipientName = null,
        category = null,
        smsId = null,
        smsThreadId = null,
        smsAddress = null,
        smsDate = null,
        transactionFingerprint = ""
    )

    @Test
    fun deriveBanks_ordersByTransactionCount() {
        val banks = BankFilter.deriveBanks(listOf(tx("HDFC"), tx("SBI"), tx("HDFC"), tx("Axis Bank")))
        assertEquals(listOf("HDFC", "SBI", "Axis Bank"), banks)
    }

    @Test
    fun deriveBanks_ignoresBlankNames() {
        val banks = BankFilter.deriveBanks(listOf(tx("HDFC"), tx(""), tx("   ")))
        assertEquals(listOf("HDFC"), banks)
    }

    @Test
    fun deriveBanks_emptyInput() {
        assertTrue(BankFilter.deriveBanks(emptyList()).isEmpty())
    }

    @Test
    fun validateBank_acceptsCaseInsensitiveRealBank() {
        assertTrue(BankFilter.validateBank(listOf("HDFC", "SBI"), "hdfc"))
    }

    @Test
    fun validateBank_rejectsUnknownBank() {
        assertFalse(BankFilter.validateBank(listOf("HDFC"), "Goldman Sachs"))
    }

    @Test
    fun validateBank_rejectsBlankInput() {
        assertFalse(BankFilter.validateBank(listOf("HDFC"), "  "))
    }

    @Test
    fun applyBankFilter_filtersToBank() {
        val txs = listOf(tx("HDFC"), tx("SBI"))
        assertEquals(listOf(tx("HDFC")), BankFilter.applyBankFilter(txs, "HDFC"))
    }

    @Test
    fun applyBankFilter_nullReturnsAll() {
        val txs = listOf(tx("HDFC"), tx("SBI"))
        assertEquals(txs, BankFilter.applyBankFilter(txs, null))
    }
}
