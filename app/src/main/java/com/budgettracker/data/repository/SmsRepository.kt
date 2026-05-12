package com.budgettracker.data.repository

import android.content.ContentResolver
import android.provider.Telephony
import com.budgettracker.domain.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val contentResolver: ContentResolver) {

    suspend fun getSmsMessages(
        startTime: Long,
        endTime: Long
    ): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.DATE_SENT,
                Telephony.Sms.TYPE
            ),
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(startTime.toString(), endTime.toString()),
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                do {
                    val address = it.getString(addressIdx) ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    val date = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)

                    // TYPE = 1 is received, TYPE = 2 is sent
                    // We want received messages (type == 1)
                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX || type == 1) {
                        messages.add(SmsMessage(address, body, date))
                    }
                } while (it.moveToNext())
            }
        }

        messages
    }

    suspend fun getAllSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                do {
                    val address = it.getString(addressIdx) ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    val date = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)

                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX || type == 1) {
                        messages.add(SmsMessage(address, body, date))
                    }
                } while (it.moveToNext())
            }
        }

        messages
    }
}