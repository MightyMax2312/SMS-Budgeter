package com.budgettracker.domain.usecase

import com.budgettracker.domain.model.SmsMessage
import com.budgettracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankMessageParserTest {

    private val parser = BankMessageParser()

    private fun sms(body: String, address: String = "HDFCBK"): SmsMessage =
        SmsMessage(id = 1L, threadId = 1L, address = address, body = body, date = 0L)

    private fun parsedAmount(body: String, address: String = "HDFCBK"): Double? =
        parser.parseBankSms(sms(body, address))?.amount

    private fun parsedType(body: String, address: String = "HDFCBK"): TransactionType? =
        parser.parseBankSms(sms(body, address))?.transactionType

    @Test
    fun avlBalanceFirst_extractsTransactionAmountNotBalance() {
        val body = "Avl Bal Rs.1,23,456.78. Rs.1,234.56 debited from A/c *1234 on 05-08-26."
        assertEquals(1234.56, parsedAmount(body)!!, 0.001)
        assertEquals(TransactionType.DEBIT, parsedType(body))
    }

    @Test
    fun normalDebit_extractsCorrectAmount() {
        val body = "Rs.1,234.56 debited from A/c *1234 on 05-08-26. Avl Bal Rs.45,678.90."
        assertEquals(1234.56, parsedAmount(body)!!, 0.001)
        assertEquals(TransactionType.DEBIT, parsedType(body))
    }

    @Test
    fun creditWithReceived_parsesAsCredit() {
        val body = "Rs.5,000.00 received from UPI. Ref 412356789012."
        assertEquals(5000.0, parsedAmount(body)!!, 0.001)
        assertEquals(TransactionType.CREDIT, parsedType(body))
    }

    @Test
    fun utrWithDecimal_isNotTakenAsAmount() {
        val body = "Rs.100.00 debited via UPI. UPI Ref 412356789012.00"
        assertEquals(100.0, parsedAmount(body)!!, 0.001)
    }

    @Test
    fun dateWithDots_isNotTakenAsAmount() {
        val body = "Your A/c *1234 was debited on 12.03.2025."
        assertNull(parser.parseBankSms(sms(body)))
    }

    @Test
    fun transferReceived_breaksTieAsCredit() {
        val body = "Rs.500.00 received via NEFT transfer from XYZ."
        assertEquals(TransactionType.CREDIT, parsedType(body))
    }

    @Test
    fun refund_isCredit() {
        val body = "Rs.500.00 refunded to your account. Ref 123456."
        assertEquals(TransactionType.CREDIT, parsedType(body))
        assertEquals(500.0, parsedAmount(body)!!, 0.001)
    }

    @Test
    fun cashback_isCredit() {
        val body = "Rs.50.00 cashback received on your card."
        assertEquals(TransactionType.CREDIT, parsedType(body))
    }

    @Test
    fun transferFromPerson_isCredit() {
        val body = "Rs.1,000.00 transferred from Rakesh via UPI."
        assertEquals(TransactionType.CREDIT, parsedType(body))
        assertEquals(1000.0, parsedAmount(body)!!, 0.001)
    }

    @Test
    fun transferFromYourAccount_isDebit() {
        val body = "Rs.1,000.00 transferred from your A/c *1234 to Rakesh."
        assertEquals(TransactionType.DEBIT, parsedType(body))
    }

    @Test
    fun transferToPerson_isDebit() {
        val body = "Rs.1,000.00 transferred to Rakesh."
        assertEquals(TransactionType.DEBIT, parsedType(body))
    }

    @Test
    fun reversal_isCredit() {
        val body = "Rs.200.00 reversal of previous transaction."
        assertEquals(TransactionType.CREDIT, parsedType(body))
    }

    @Test
    fun mandate_isSkipped() {
        val body = "UPI Mandate created for Rs.500.00 on 05-08-26."
        assertNull(parser.parseBankSms(sms(body)))
    }

    @Test
    fun multipleRs_picksAmountNearTransactionKeyword() {
        val body = "Avl Bal Rs.2,00,000.00. Rs.999.00 debited. Avl Bal Rs.1,99,001.00."
        assertEquals(999.0, parsedAmount(body)!!, 0.001)
    }
}
