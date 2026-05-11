package com.example.m_dailyplanner.notification

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.ui.theme.MDailyPlannerTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskId = intent.getIntExtra(AlarmReceiver.EXTRA_TASK_ID, -1)
        val taskName = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_NAME) ?: ""
        val taskDescription = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_DESCRIPTION) ?: ""

        startAlarmSound()

        setContent {
            MDailyPlannerTheme {
                AlarmScreen(
                    taskName = taskName,
                    taskDescription = taskDescription,
                    onDismiss = {
                        stopAlarmSound()
                        NotificationHelper(this).cancelNotification(taskId)
                        finish()
                    },
                    onSnooze = {
                        stopAlarmSound()
                        val stubTask = Task(
                            id = taskId,
                            name = taskName,
                            description = taskDescription,
                            date = "",
                            time = "",
                            reminderEnabled = true,
                            status = "PENDING"
                        )
                        ReminderScheduler.scheduleReminder(this, stubTask, snoozeMinutes = 15)
                        NotificationHelper(this).cancelNotification(taskId)
                        finish()
                    }
                )
            }
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // alarm shows visually without sound if media fails
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
}

@Composable
private fun AlarmScreen(
    taskName: String,
    taskDescription: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf(currentTimeString()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = currentTimeString()
        }
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentTime,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = taskName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (taskDescription.isNotEmpty()) {
                    Text(
                        text = taskDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Dismiss", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Snooze 15 min", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun currentTimeString(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
