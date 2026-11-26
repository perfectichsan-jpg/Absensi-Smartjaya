package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceStatus
import com.example.data.model.UiState
import com.example.data.model.WorkMode
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.DateTimeFormatters

private enum class HistoryFilter(val label: String) {
  ALL("Semua"),
  ON_TIME("Tepat Waktu"),
  LATE("Terlambat"),
  WFO("WFO"),
  WFH("WFH"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
  logsState: UiState<List<AttendanceLog>>,
  isSubmitting: Boolean,
  onUpdateActivities: (logId: String, notes: String, onSuccess: () -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
  var editingLog by remember { mutableStateOf<AttendanceLog?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Riwayat Absensi & Logbook",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        HistoryFilter.entries.forEach { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { selectedFilter = filter },
            label = { Text(filter.label) },
            modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}"),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (logsState) {
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
              text = logsState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          val filteredLogs =
            remember(logsState.data, selectedFilter) {
              when (selectedFilter) {
                HistoryFilter.ALL -> logsState.data
                HistoryFilter.ON_TIME ->
                  logsState.data.filter { it.statusEnum == AttendanceStatus.ON_TIME }
                HistoryFilter.LATE ->
                  logsState.data.filter { it.statusEnum == AttendanceStatus.LATE }
                HistoryFilter.WFO -> logsState.data.filter { it.workModeEnum == WorkMode.WFO }
                HistoryFilter.WFH -> logsState.data.filter { it.workModeEnum == WorkMode.WFH }
              }
            }

          if (filteredLogs.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.History,
              title = "Belum ada catatan absensi",
              subtitle = "Lakukan absen masuk di tab Beranda untuk mulai mencatat kehadiran harian.",
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filteredLogs, key = { it.id }) { log ->
                AttendanceHistoryCard(
                  log = log,
                  onClick = { editingLog = log },
                )
              }
            }
          }
        }
      }
    }
  }

  editingLog?.let { log ->
    var notes by remember(log.id) { mutableStateOf(log.dailyActivities) }
    AlertDialog(
      onDismissRequest = { if (!isSubmitting) editingLog = null },
      title = {
        Text("Logbook ${DateTimeFormatters.formatShortDate(log.dateKey)}")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text =
              "Masuk ${DateTimeFormatters.formatTimeHHmm(log.checkInTime)} • Pulang ${
                DateTimeFormatters.formatTimeHHmm(log.checkOutTime)
              }",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it.take(500) },
            label = { Text("Aktivitas Harian") },
            minLines = 4,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth().testTag("edit_history_logbook_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateActivities(log.id, notes) { editingLog = null }
          },
          enabled = !isSubmitting,
          modifier = Modifier.testTag("save_history_logbook_button"),
        ) {
          Text(if (isSubmitting) "Menyimpan..." else "Simpan")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { editingLog = null },
          enabled = !isSubmitting,
        ) {
          Text("Tutup")
        }
      },
    )
  }
}

@Composable
private fun AttendanceHistoryCard(
  log: AttendanceLog,
  onClick: () -> Unit,
) {
  val isOnTime = log.statusEnum == AttendanceStatus.ON_TIME
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag("history_card_${log.dateKey}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = DateTimeFormatters.formatShortDate(log.dateKey),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StatusPill(
            text = log.workModeEnum.label,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          StatusPill(
            text = log.statusEnum.label,
            containerColor =
              if (isOnTime) StatusOnTimeColor.copy(alpha = 0.14f)
              else StatusLateColor.copy(alpha = 0.14f),
            contentColor = if (isOnTime) StatusOnTimeColor else StatusLateColor,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Masuk",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = DateTimeFormatters.formatTimeHHmm(log.checkInTime),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Pulang",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = DateTimeFormatters.formatTimeHHmm(log.checkOutTime),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Durasi",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = DateTimeFormatters.formatDuration(log.checkInTime, log.checkOutTime),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (log.locationLabel.isNotBlank() || log.dailyActivities.isNotBlank()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }

      if (log.locationLabel.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = log.locationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (log.dailyActivities.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Notes,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(15.dp).padding(top = 2.dp),
          )
          Text(
            text = log.dailyActivities,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
