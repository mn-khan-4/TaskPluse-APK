package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.data.model.TaskType
import com.example.ui.theme.ElegantBackground
import com.example.ui.theme.ElegantBorder
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(
    itemToEdit: TaskPulseItem?,
    onSave: (TaskPulseItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.taskCategory ?: TaskCategory.PERSONAL) }
    var selectedType by remember { mutableStateOf(itemToEdit?.taskType ?: TaskType.TASK) }
    var selectedPriority by remember { mutableStateOf(itemToEdit?.taskPriority ?: TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf(itemToEdit?.dueDate) }
    var reminderTime by remember { mutableStateOf(itemToEdit?.reminderTime) }
    var amountStr by remember { mutableStateOf(itemToEdit?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var billPayee by remember { mutableStateOf(itemToEdit?.billPayee ?: "") }
    var isRecurring by remember { mutableStateOf(itemToEdit?.isRecurring ?: false) }
    var recurringInterval by remember { mutableStateOf(itemToEdit?.recurringInterval ?: "MONTHLY") }
    var syncToGoogleCalendar by remember { mutableStateOf(itemToEdit?.isSyncedToCalendar ?: true) }

    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ElegantSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (itemToEdit == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Water plants, Pay internet bill") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_task_title"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElegantPrimary,
                    unfocusedBorderColor = ElegantBorder,
                    focusedContainerColor = ElegantBackground,
                    unfocusedContainerColor = ElegantBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Details (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_task_description"),
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElegantPrimary,
                    unfocusedBorderColor = ElegantBorder,
                    focusedContainerColor = ElegantBackground,
                    unfocusedContainerColor = ElegantBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type Selection (Task, To-Do, Bill)
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElegantPrimary,
                            selectedLabelColor = Color(0xFF1C1B1F),
                            containerColor = ElegantBackground,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ElegantPrimary else ElegantBorder
                        )
                    )
                }
            }

            // Bill specific fields
            if (selectedType == TaskType.BILL || selectedCategory == TaskCategory.BILLS) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ElegantPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bill_amount"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = ElegantBorder,
                            focusedContainerColor = ElegantBackground,
                            unfocusedContainerColor = ElegantBackground
                        )
                    )
                    OutlinedTextField(
                        value = billPayee,
                        onValueChange = { billPayee = it },
                        label = { Text("Payee / Vendor") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_bill_payee"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = ElegantBorder,
                            focusedContainerColor = ElegantBackground,
                            unfocusedContainerColor = ElegantBackground
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskCategory.entries.take(4).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getCategoryColor(cat),
                            selectedLabelColor = Color(0xFF1C1B1F),
                            containerColor = ElegantBackground,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) getCategoryColor(cat) else ElegantBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selection
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElegantPrimary,
                            selectedLabelColor = Color(0xFF1C1B1F),
                            containerColor = ElegantBackground,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ElegantPrimary else ElegantBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date Picker Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance()
                        dueDate?.let { cal.timeInMillis = it }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(Calendar.YEAR, y)
                                cal.set(Calendar.MONTH, m)
                                cal.set(Calendar.DAY_OF_MONTH, d)
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(Calendar.HOUR_OF_DAY, hour)
                                        cal.set(Calendar.MINUTE, minute)
                                        cal.set(Calendar.SECOND, 0)
                                        dueDate = cal.timeInMillis
                                        if (reminderTime == null) {
                                            reminderTime = cal.timeInMillis
                                        }
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                shape = RoundedCornerShape(14.dp),
                color = ElegantBackground,
                border = BorderStroke(1.dp, ElegantBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = ElegantPrimary)
                        Column {
                            Text(
                                text = "Due Date & Time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dueDate?.let { dateFormat.format(Date(it)) } ?: "Not set (Tap to set)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (dueDate != null) {
                        IconButton(
                            onClick = { dueDate = null; reminderTime = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear date", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recurring Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = ElegantPrimary)
                    Text("Recurring Reminder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1C1B1F),
                        checkedTrackColor = ElegantPrimary,
                        uncheckedBorderColor = ElegantBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Google Calendar Sync Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElegantPrimary)
                    Column {
                        Text("Add to Google Calendar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Sync deadline to your calendar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = syncToGoogleCalendar,
                    onCheckedChange = { syncToGoogleCalendar = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1C1B1F),
                        checkedTrackColor = ElegantPrimary,
                        uncheckedBorderColor = ElegantBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedAmt = amountStr.toDoubleOrNull()
                        val updated = (itemToEdit ?: TaskPulseItem(title = title)).copy(
                            title = title,
                            description = description,
                            category = selectedCategory.name,
                            type = selectedType.name,
                            priority = selectedPriority.name,
                            dueDate = dueDate,
                            reminderTime = reminderTime ?: dueDate,
                            amount = parsedAmt,
                            billPayee = if (billPayee.isNotBlank()) billPayee else null,
                            isRecurring = isRecurring,
                            recurringInterval = if (isRecurring) recurringInterval else null,
                            isSyncedToCalendar = syncToGoogleCalendar
                        )
                        onSave(updated)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantPrimary,
                    contentColor = Color(0xFF1C1B1F)
                )
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
