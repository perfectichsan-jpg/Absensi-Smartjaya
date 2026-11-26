package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceStatus
import com.example.data.model.LeaveRequest
import com.example.data.model.LeaveStatus
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.ui.theme.StatusRejectedColor
import com.example.util.DateTimeFormatters

@Composable
fun SupervisorScreen(
  adminAttendanceState: UiState<List<AttendanceLog>>,
  adminLeaveState: UiState<List<LeaveRequest>>,
  isSubmitting: Boolean,
  onReviewLeave: (
    requestId: String,
    newStatus: LeaveStatus,
    note: String,
    onSuccess: () -> Unit,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }
  var reviewTarget by remember { mutableStateOf<Pair<LeaveRequest, LeaveStatus>?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize().padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Panel Pembimbing Magang",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Pantau absensi peserta dan proses persetujuan izin.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))

      TabRow(selectedTabIndex = selectedSubTab) {
        Tab(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          text = { Text("Persetujuan Izin") },
        )
        Tab(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          text = { Text("Absensi Peserta") },
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedSubTab == 0) {
        when (adminLeaveState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> {
            Text(text = adminLeaveState.message, color = MaterialTheme.colorScheme.error)
          }
          is UiState.Success -> {
            if (adminLeaveState.data.isEmpty()) {
              EmptyStateView(
                icon = Icons.Default.VerifiedUser,
                title = "Belum ada pengajuan izin peserta",
                subtitle = "Semua pengajuan izin atau sakit anak magang akan muncul di sini.",
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(adminLeaveState.data, key = { it.id }) { req ->
                  AdminLeaveCard(
                    request = req,
                    isSubmitting = isSubmitting,
                    onApprove = { reviewTarget = req to LeaveStatus.APPROVED },
                    onReject = { reviewTarget = req to LeaveStatus.REJECTED },
                  )
                }
              }
            }
          }
        }
      } else {
        when (adminAttendanceState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> {
            Text(text = adminAttendanceState.message, color = MaterialTheme.colorScheme.error)
          }
          is UiState.Success -> {
            if (adminAttendanceState.data.isEmpty()) {
              EmptyStateView(
                icon = Icons.Default.Groups,
                title = "Belum ada data kehadiran",
                subtitle = "Catatan check-in anak magang akan tampil secara real-time.",
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(adminAttendanceState.data, key = { it.id }) { log ->
                  AdminAttendanceCard(log)
                }
              }
            }
          }
        }
      }
    }
  }

  reviewTarget?.let { (request, targetStatus) ->
    var note by remember(request.id, targetStatus) { mutableStateOf("") }
    val isApprove = targetStatus == LeaveStatus.APPROVED
    AlertDialog(
      onDismissRequest = { if (!isSubmitting) reviewTarget = null },
      title = {
        Text(if (isApprove) "Setujui Pengajuan Izin" else "Tolak Pengajuan Izin")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "${request.internName} (${request.division}) • ${request.leaveTypeEnum.label}",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = request.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(200) },
            label = { Text("Catatan Pembimbing (Opsional)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onReviewLeave(request.id, targetStatus, note) { reviewTarget = null }
          },
          enabled = !isSubmitting,
        ) {
          Text(if (isApprove) "Setujui" else "Tolak")
        }
      },
      dismissButton = {
        TextButton(onClick = { reviewTarget = null }, enabled = !isSubmitting) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun AdminLeaveCard(
  request: LeaveRequest,
  isSubmitting: Boolean,
  onApprove: () -> Unit,
  onReject: () -> Unit,
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = request.internName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${request.division} • ${request.leaveTypeEnum.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = request.statusEnum.label,
          containerColor = pillBg,
          contentColor = pillFg,
        )
      }

      Text(
        text = "${request.startDate} s/d ${request.endDate}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(text = request.reason, style = MaterialTheme.typography.bodyMedium)

      if (request.statusEnum == LeaveStatus.PENDING) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onReject,
            enabled = !isSubmitting,
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f).testTag("reject_leave_${request.id}"),
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tolak")
          }
          Button(
            onClick = onApprove,
            enabled = !isSubmitting,
            colors =
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.weight(1f).testTag("approve_leave_${request.id}"),
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Setujui")
          }
        }
      }
    }
  }
}

@Composable
private fun AdminAttendanceCard(log: AttendanceLog) {
  val isOnTime = log.statusEnum == AttendanceStatus.ON_TIME
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = log.internName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${log.division} • ${DateTimeFormatters.formatShortDate(log.dateKey)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = log.statusEnum.label,
          containerColor =
            if (isOnTime) StatusOnTimeColor.copy(alpha = 0.14f)
            else StatusLateColor.copy(alpha = 0.14f),
          contentColor = if (isOnTime) StatusOnTimeColor else StatusLateColor,
        )
      }
      Text(
        text =
          "${log.workModeEnum.label} • Masuk ${DateTimeFormatters.formatTimeHHmm(log.checkInTime)} • Pulang ${
            DateTimeFormatters.formatTimeHHmm(log.checkOutTime)
          }",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      if (log.dailyActivities.isNotBlank()) {
        Text(
          text = log.dailyActivities,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
