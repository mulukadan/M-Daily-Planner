package com.example.m_dailyplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var step by remember { mutableStateOf(1) }

    val content = when (step) {
        1 -> OnboardingContent(
            title = "Welcome to DayFlow",
            description = "Organize your life one day at a time with our intuitive planner."
        )
        2 -> OnboardingContent(
            title = "Stay on Track",
            description = "Set reminders and categorize your tasks to boost your productivity."
        )
        else -> OnboardingContent(
            title = "Never Forget",
            description = "Unfinished tasks automatically carry forward to the next day."
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(text = if (step < 3) "Next" else "Get Started")
            }
        }
    }
}

data class OnboardingContent(val title: String, val description: String)
