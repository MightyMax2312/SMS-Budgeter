package com.budgettracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            Text("SMS Permission Required",
                fontSize = 22.sp, fontWeight = FontWeight(700),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))

            Text("This app needs SMS access to detect bank transactions automatically.",
                fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                lineHeight = 24.sp)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Grant Permission")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}