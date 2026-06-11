package com.budgettracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.SearchManager
import android.net.Uri
import android.provider.Telephony
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagePopup(
    rawMessage: String,
    timestamp: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(36.dp, 5.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.12f))
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Original Message",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = rawMessage,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", rawMessage))
                            showCopied = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (showCopied) "Copied!" else "Copy")
                    }

                    Button(
                        onClick = { openClickedSms(context, rawMessage, timestamp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open SMS")
                    }
                }

                Spacer(Modifier.height(16.dp))

                val (bank, amount, isCredit) = parseDetails(rawMessage)
                val searchKey = buildSearchLiteral(rawMessage, null)

                DetailRow("Bank", bank)
                DetailRow("Type", if (isCredit) "Credited" else "Debited")
                DetailRow("Amount", "Rs${String.format("%.2f", amount)}", isCredit)
                DetailRow("Time", formatPopupTimestamp(timestamp))
                DetailRow("Search key", searchKey)

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "All data stays on your device.\nNever sent to any server.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun openClickedSms(context: Context, rawMessage: String, timestamp: Long) {
    val smsTarget = findSmsTarget(context, rawMessage, timestamp)
    val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
    val searchLiteral = buildSearchLiteral(rawMessage, smsTarget?.address)
    val searchIntents = listOfNotNull(
        defaultSmsPackage?.let { packageName ->
            Intent(Intent.ACTION_SEARCH).apply {
                setPackage(packageName)
                putExtra(SearchManager.QUERY, searchLiteral)
                putExtra(SearchManager.USER_QUERY, searchLiteral)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        },
        Intent(Intent.ACTION_SEARCH).apply {
            putExtra(SearchManager.QUERY, searchLiteral)
            putExtra(SearchManager.USER_QUERY, searchLiteral)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
    val specificIntents = listOfNotNull(
        smsTarget?.messageId?.let { messageId ->
            Intent(Intent.ACTION_VIEW, Uri.parse("content://sms/$messageId")).apply {
                defaultSmsPackage?.let { setPackage(it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        },
        smsTarget?.threadId?.let { threadId ->
            Intent(Intent.ACTION_VIEW, Uri.parse("content://mms-sms/conversations/$threadId")).apply {
                defaultSmsPackage?.let { setPackage(it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        },
        smsTarget?.address?.takeIf { it.isNotBlank() }?.let { address ->
            Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${Uri.encode(address)}")).apply {
                defaultSmsPackage?.let { setPackage(it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    )
    val fallbackIntents = listOf(
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            defaultSmsPackage?.let { setPackage(it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    for (intent in searchIntents + specificIntents + fallbackIntents) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try the next inbox-style intent.
        }
    }
}

private fun findSmsTarget(context: Context, rawMessage: String, timestamp: Long): SmsTarget? {
    val windowMillis = 5 * 60 * 1000L
    return querySmsTarget(
        context = context,
        selection = "${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} BETWEEN ? AND ?",
        selectionArgs = arrayOf(
            rawMessage,
            (timestamp - windowMillis).toString(),
            (timestamp + windowMillis).toString()
        )
    ) ?: querySmsTarget(
        context = context,
        selection = "${Telephony.Sms.BODY} = ?",
        selectionArgs = arrayOf(rawMessage)
    )
}

private fun querySmsTarget(
    context: Context,
    selection: String,
    selectionArgs: Array<String>
): SmsTarget? {
    val projection = arrayOf(
        Telephony.Sms._ID,
        "thread_id",
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )

    return try {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIndex = cursor.getColumnIndexOrThrow("thread_id")
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            SmsTarget(
                messageId = cursor.getLong(idIndex),
                threadId = cursor.getLong(threadIndex),
                address = cursor.getString(addressIndex).orEmpty()
            )
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatPopupTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"
    val formatter = SimpleDateFormat("dd MMM yyyy, h:mm:ss a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun buildSearchLiteral(rawMessage: String, sender: String?): String {
    val compactMessage = rawMessage.replace(Regex("\\s+"), " ").trim()
    val currencyAmount = Regex(
        """(?:INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""",
        RegexOption.IGNORE_CASE
    ).findAll(compactMessage)
        .map { it.value.trim() }
        .maxByOrNull { it.count(Char::isDigit) }
    val fallbackAmount = Regex("""[\d,]+(?:\.\d{1,2})""")
        .findAll(compactMessage)
        .map { it.value.trim() }
        .filter { candidate -> candidate.count(Char::isDigit) >= 3 }
        .maxByOrNull { it.count(Char::isDigit) }
    val amountLiteral = currencyAmount ?: fallbackAmount
    val uniqueToken = extractUniqueSearchToken(compactMessage)
    val senderToken = sender
        ?.replace(Regex("[^A-Za-z0-9]"), "")
        ?.takeIf { it.length >= 4 }

    if (amountLiteral != null && uniqueToken != null) {
        return "$amountLiteral $uniqueToken"
    }

    if (uniqueToken != null) {
        return uniqueToken
    }

    if (amountLiteral != null && senderToken != null) {
        return "$senderToken $amountLiteral"
    }

    return if (compactMessage.length <= 80) {
        compactMessage
    } else {
        amountLiteral ?: compactMessage.take(80)
    }
}

private fun extractUniqueSearchToken(message: String): String? {
    val referencePatterns = listOf(
        """(?i)\b(?:utr|rrn|ref(?:erence)?(?:\s*no)?|txn(?:\s*id)?|transaction\s*id|upi\s*ref(?:\s*no)?|imps\s*ref(?:\s*no)?)\b[\s:.\-#]*(?:no\.?\s*)?([A-Z0-9][A-Z0-9\-\/]{5,})""",
        """(?i)\b(?:vpa|upi)\b[\s:.\-#]*([A-Z0-9._\-]+@[A-Z0-9._\-]+)"""
    )

    for (pattern in referencePatterns) {
        Regex(pattern).find(message)?.groupValues?.getOrNull(1)
            ?.trim('.', ',', ';', ':', '-', '/', ' ')
            ?.takeIf { it.length >= 6 }
            ?.let { return it }
    }

    val ignored = setOf(
        "credited", "credit", "debited", "debit", "account", "available",
        "balance", "transaction", "received", "deposit", "payment", "bank",
        "amount", "inr", "rs", "with", "from", "your"
    )

    return Regex("""\b[A-Z0-9][A-Z0-9\-\/]{5,}\b""", RegexOption.IGNORE_CASE)
        .findAll(message)
        .map { it.value.trim('.', ',', ';', ':', '-', '/', ' ') }
        .filter { token ->
            val lower = token.lowercase(Locale.getDefault())
            token.any(Char::isLetter) &&
                token.any(Char::isDigit) &&
                token.count(Char::isDigit) >= 3 &&
                lower !in ignored
        }
        .maxByOrNull { it.length }
}

private data class SmsTarget(
    val messageId: Long,
    val threadId: Long,
    val address: String
)

@Composable
private fun DetailRow(label: String, value: String, isPositive: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val valueColor = if (isPositive) Color(0xFF2D6A4F) else MaterialTheme.colorScheme.error
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

private fun parseDetails(msg: String): Triple<String, Double, Boolean> {
    val lower = msg.lowercase()
    var bank = "Unknown Bank"
    for ((keyword, name) in mapOf(
        "hdfc" to "HDFC Bank",
        "icici" to "ICICI Bank",
        "sbi" to "State Bank",
        "axis" to "Axis Bank",
        "kotak" to "Kotak Bank",
        "pnb" to "Punjab National Bank",
        "yes bank" to "Yes Bank",
        "bank of baroda" to "Bank of Baroda"
    )) {
        if (lower.contains(keyword)) {
            bank = name
            break
        }
    }

    val regex = """(?:INR|Rs\.?)\s*([\d,]+(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)
    val amount = regex.find(msg)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    val isCredit = lower.contains("credit") || lower.contains("deposit") || lower.contains("received")
    return Triple(bank, amount, isCredit)
}
