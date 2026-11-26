package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LeaveRequest
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.ui.theme.StatusRejectedColor
import com.example.util.DateTimeFormatters

@Composable
fun LeaveScreen(
  leavesState: UiState<List<LeaveRequest>>,
  isSubmitting: Boolean,
  onSubmitLeave: (
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    reason: String,
    onSuccess: () -> Unit,
  ) -> Unit,
  onCancelLeave: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Pengajuan Izin & Sakit",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Ajukan izin bimbingan kampus, sakit, atau keperluan pribadi.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(14.dp))

      when (leavesState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(
              text = leavesState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          if (leavesState.data.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.EventBusy,
              title = "Belum ada pengajuan izin",
              subtitle = "Gunakan tombol Ajukan Izin di kanan bawah jika berhalangan hadir.",
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(bottom = 88.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(leavesState.data, key = { it.id }) { request ->
                LeaveRequestCard(
                  request = request,
                  isSubmitting = isSubmitting,
                  onCancel = { onCancelLeave(request.id) },
                )
              }
            }
          }
        }
      }
    }

    ExtendedFloatingActionButton(
      onClick = { showCreateDialog = true },
      icon = { Icon(Icons.Default.Add, contentDescription = null) },
      text = { Text("Ajukan Izin") },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(20.dp)
          .testTag("create_leave_button"),
    )
  }

  if (showCreateDialog) {
    CreateLeaveDialog(
      isSubmitting = isSubmitting,
      onDismiss = { showCreateDialog = false },
      onSubmit = { type, start, end, reason ->
        onSubmitLeave(type, start, end, reason) { showCreateDialog = false }
      },
    )
  }
}

@Composable
private fun LeaveRequestCard(
  request: LeaveRequest,
  isSubmitting: Boolean,
  onCancel: () -> Unit,
) {
  val (pillBg, pillFg) =
    when (request.statusEnum) {
      LeaveStatus.APPROVED -> StatusOnTimeColor.copy(alpha = 0.14f) to StatusOnTimeColor
      LeaveStatus.REJECTED -> StatusRejectedColor.copy(alpha = 0.14f) to StatusRejectedColor
      LeaveStatus.PENDING -> StatusLateColor.copy(alpha = 0.14f) to StatusLateColor
    }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = request.leaveTypeEnum.label,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StatusPill(
            text = request.statusEnum.label,
            containerColor = pillBg,
            contentColor = pillFg,
          )
          if (request.statusEnum == LeaveStatus.PENDING) {
            IconButton(
              onClick = onCancel,
              enabled = !isSubmitting,
              modifier = Modifier.size(36.dp).testTag("cancel_leave_${request.id}"),
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Batalkan pengajuan",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      Text(
        text =
          if (request.startDate == request.endDate)
            DateTimeFormatters.formatShortDate(request.startDate)
          else
            "${DateTimeFormatters.formatShortDate(request.startDate)} – ${
              DateTimeFormatters.formatShortDate(request.endDate)
            }",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )

      Text(
        text = request.reason,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (request.reviewerNote.isNotBlank()) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(
            text = "Catatan Pembimbing: ${request.reviewerNote}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateLeaveDialog(
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (LeaveType, String, String, String) -> Unit,
) {
  var selectedType by remember { mutableStateOf(LeaveType.IZIN_KAMPUS) }
  var startDate by remember { mutableStateOf(DateTimeFormatters.currentDateKey()) }
  var endDate by remember { mutableStateOf(DateTimeFormatters.currentDateKey()) }
  var reason by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  var pickingStart by remember { mutableStateOf(false) }
  var pickingEnd by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    title = { Text("Ajukan Izin / Sakit") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Jenis Pengajuan",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          LeaveType.entries.forEach { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type.label) },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { pickingStart = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(startDate, style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { pickingEnd = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(endDate, style = MaterialTheme.typography.labelSmall)
          }
        }

        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it.take(300) },
          label = { Text("Alasan / Keterangan") },
          placeholder = { Text("Minimal 5 karakter...") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("leave_reason_input"),
        )

        if (errorText != null) {
          Text(
            text = errorText.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (endDate < startDate) {
            errorText = "Tanggal selesai tidak boleh sebelum tanggal mulai."
            return@Button
          }
          if (reason.trim().length < 5) {
            errorText = "Alasan izin minimal 5 karakter."
            return@Button
          }
          errorText = null
          onSubmit(selectedType, startDate, endDate, reason)
        },
        enabled = !isSubmitting,
        modifier = Modifier.testTag("submit_leave_button"),
      ) {
        Text(if (isSubmitting) "Mengirim..." else "Kirim Pengajuan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSubmitting) {
        Text("Batal")
      }
    },
  )

  if (pickingStart) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { pickingStart = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              startDate = DateTimeFormatters.currentDateKey(millis)
              if (endDate < startDate) endDate = startDate
            }
            pickingStart = false
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { pickingStart = false }) { Text("Batal") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (pickingEnd) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { pickingEnd = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              endDate = DateTimeFormatters.currentDateKey(millis)
            }
            pickingEnd = false
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { pickingEnd = false }) { Text("Batal") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
