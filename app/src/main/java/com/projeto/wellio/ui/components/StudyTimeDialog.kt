package com.projeto.wellio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun StudyTimerDialog(
    timeString: String,
    onStopClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FOCADO",
                    color = Color(0xFF7C4DFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = timeString,
                    color = Color.Black,
                    fontWeight = FontWeight.Light,
                    fontSize = 56.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onStopClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("ENCERRAR SESSÃO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}