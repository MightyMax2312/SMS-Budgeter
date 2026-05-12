package com.budgettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onImportClick: (Long) -> Unit,
    isLoading: Boolean
) {
    var selectedDays by remember { mutableLongStateOf(30L) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Text("Welcome to", fontSize = 22.sp, fontWeight = FontWeight(400),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("SMS Budget Tracker", fontSize = 28.sp, fontWeight = FontWeight(700),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))

            Text("Automatically track your bank transactions from SMS messages.",
                fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.weight(1f, fill = false))

            Text("How far back should we import?", fontSize = 18.sp, fontWeight = FontWeight(600))

            Spacer(Modifier.height(16.dp))

            for ((days, label) in listOf(7L to "Last 7 days", 30L to "Last 30 days",
                90L to "Last 3 months", 365L to "Last year")) {
                ImportOpt(label, selectedDays == days) { selectedDays = days }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onImportClick(selectedDays) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Text("Start Import")
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            Text("Your data stays on your device.",
                fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ImportOpt(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = if (selected) FontWeight(600) else FontWeight(400))
            if (selected) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}