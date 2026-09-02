package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.ParsedTaskResult
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.ElegantBackground
import com.example.ui.theme.ElegantBorder
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantPrimaryContainer
import com.example.ui.theme.ElegantSurface
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.TaskPulseViewModel
import com.example.voice.VoiceState

@Composable
fun VoiceInputDialog(
    viewModel: TaskPulseViewModel,
    onDismiss: () -> Unit
) {
    val voiceState by viewModel.speechHelper.voiceState.collectAsState()
    val partialTranscript by viewModel.speechHelper.partialTranscript.collectAsState()
    val rmsDb by viewModel.speechHelper.rmsDb.collectAsState()
    val parsedPreview by viewModel.parsedTaskPreview.collectAsState()
    val isAiParsing by viewModel.isAiParsing.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }
    var isManualMode by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState is VoiceState.Listening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("voice_input_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = ElegantSurface,
            border = BorderStroke(1.dp, ElegantBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElegantPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Voice Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElegantPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("voice_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone Animated Visualizer
                Box(
                    modifier = Modifier
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulse ring
                    if (voiceState is VoiceState.Listening) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(ElegantPrimary.copy(alpha = 0.2f))
                        )
                    }

                    // Main mic container
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (voiceState is VoiceState.Listening) ElegantPrimaryContainer else ElegantBorder)
                            .clickable {
                                if (voiceState is VoiceState.Listening) {
                                    viewModel.speechHelper.stopListening()
                                } else {
                                    viewModel.speechHelper.startListening()
                                }
                            }
                            .testTag("voice_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voiceState is VoiceState.Listening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone",
                            tint = if (voiceState is VoiceState.Listening) ElegantPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // State Text / Instructions
                val statusText = when (voiceState) {
                    is VoiceState.Listening -> "Listening... speak naturally"
                    is VoiceState.Processing -> "Analyzing speech..."
                    is VoiceState.Success -> "Recognized!"
                    is VoiceState.Error -> (voiceState as VoiceState.Error).message
                    else -> "Tap microphone to speak"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (voiceState is VoiceState.Listening) ElegantPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Live Transcript Preview
                if (partialTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ElegantBackground,
                        border = BorderStroke(1.dp, ElegantBorder)
                    ) {
                        Text(
                            text = "\"$partialTranscript\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // AI Parsing Spinner
                if (isAiParsing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElegantPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Categorizing & detecting due dates...",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElegantPrimary
                        )
                    }
                }

                // Parsed Structured Result Card
                parsedPreview?.let { parsed ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = ElegantBackground,
                        border = BorderStroke(1.dp, ElegantPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = getCategoryBgColor(parsed.category),
                                    border = BorderStroke(1.dp, getCategoryColor(parsed.category).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = getCategoryEmoji(parsed.category), fontSize = 12.sp)
                                        Text(
                                            text = parsed.category.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = getCategoryColor(parsed.category)
                                        )
                                    }
                                }

                                if (parsed.amount != null) {
                                    Text(
                                        text = "$${String.format(java.util.Locale.US, "%.2f", parsed.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CategoryBills
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = parsed.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (parsed.confidenceNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 ${parsed.confidenceNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.speechHelper.startListening()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantBorder,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }

                        Button(
                            onClick = {
                                viewModel.confirmSaveParsedTask(parsed)
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("confirm_save_voice_task"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantPrimary,
                                contentColor = Color(0xFF1C1B1F)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Reminder", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Fallback Manual Text Input toggle
                if (parsedPreview == null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isManualMode) "Switch to Voice" else "Or type with keyboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElegantPrimary,
                        modifier = Modifier
                            .clickable { isManualMode = !isManualMode }
                            .padding(4.dp)
                    )

                    AnimatedVisibility(visible = isManualMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            OutlinedTextField(
                                value = manualTextInput,
                                onValueChange = { manualTextInput = it },
                                placeholder = { Text("e.g. Pay electricity bill $85 next Friday") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_reminder_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElegantPrimary,
                                    unfocusedBorderColor = ElegantBorder,
                                    focusedContainerColor = ElegantBackground,
                                    unfocusedContainerColor = ElegantBackground
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (manualTextInput.isNotBlank()) {
                                        viewModel.parseAndPreviewVoiceInput(manualTextInput)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("parse_manual_text_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElegantPrimary,
                                    contentColor = Color(0xFF1C1B1F)
                                )
                            ) {
                                Text("Analyze & Schedule", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
