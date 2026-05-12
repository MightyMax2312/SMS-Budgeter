package com.budgettracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun MessagePopup(rawMessage: String, onDismiss: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    Modifier.size(36.dp, 5.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.12f))
                )

                Spacer(Modifier.height(20.dp))

                Text("Original Message",
                    fontSize = 18.sp, fontWeight = FontWeight(600),
                    modifier = Modifier.padding(bottom = 16.dp))

                // Scrollable message card
                Card(
                    Modifier.fillMaxWidth().heightIn(max = 300.dp),
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
                        Text(rawMessage,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", rawMessage))
                            showCopied = true
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (showCopied) "✓ Copied!" else "Copy")
                    }

                    // Open in SMS button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("sms:")
                                intent.putExtra("sms_body", rawMessage)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN)
                                    intent.addCategory(Intent.CATEGORY_APP_MESSAGING)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open SMS")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Parsed details
                val (bank, amount, isCredit) = parseDetails(rawMessage)

                DetailRow("Bank", bank)
                DetailRow("Type", if (isCredit) "Credited" else "Debited")
                DetailRow("Amount", "₹${String.format("%.2f", amount)}", isCredit)

                Spacer(Modifier.height(16.dp))

                Text(
                    "All data stays on your device.\nNever sent to any server.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight(600))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isPositive: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val c = if (isPositive) Color(0xFF2D6A4F) else MaterialTheme.colorScheme.error
        Text(value, fontSize = 15.sp, fontWeight = FontWeight(600), color = c)
    }
}

private fun parseDetails(msg: String): Triple<String, Double, Boolean> {
    val lower = msg.lowercase()
    var bank = "Unknown Bank"
    for ((kw, name) in mapOf(
        "hdfc" to "HDFC Bank", "icici" to "ICICI Bank",
        "sbi" to "State Bank", "axis" to "Axis Bank",
        "kotak" to "Kotak Bank", "pnb" to "Punjab National Bank",
        "yes bank" to "Yes Bank", "bank of baroda" to "Bank of Baroda"
    )) {
        if (lower.contains(kw)) { bank = name; break }
    }
    val regex = """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)
    val amt = regex.find(msg)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    val isCredit = lower.contains("credit") || lower.contains("deposit") || lower.contains("received")
    return Triple(bank, amt, isCredit)
}