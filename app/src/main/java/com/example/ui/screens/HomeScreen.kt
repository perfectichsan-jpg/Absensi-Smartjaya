package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceStatus
import com.example.data.model.InternProfile
import com.example.data.model.LeaveRequest
import com.example.data.model.SessionState
import com.example.data.model.UiState
import com.example.data.model.WorkMode
import com.example.ui.components.StatusPill
import com.example.ui.components.SummaryStatCard
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.ui.theme.StatusWorkingColor
import com.example.util.DateTimeFormatters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  profileState: UiState<InternProfile?>,
  logsState: UiState<List<AttendanceLog>>,
  leavesState: UiState<List<LeaveRequest>>,
  currentTimeMillis: Long,
  displayNameFallback: String,
  isSubmitting: Boolean,
  onOpenProfileEditor: () -> Unit,
  onCheckIn: (WorkMode, String, String) -> Unit,
  onCheckOut: (String, String) -> Unit,
  onUpdateActivities: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val profile = (profileState as? UiState.Success)?.data
  val logs = (logsState as? UiState.Success)?.data.orEmpty()
  val leaves = (leavesState as? UiState.Success)?.data.orEmpty()

  val todayKey = DateTimeFormatters.currentDateKey(currentTimeMillis)
  val todayLog = logs.firstOrNull { it.dateKey == todayKey }

  var selectedMode by remember(profile?.workModeDefault) {
    mutableStateOf(profile?.workModeEnum ?: WorkMode.WFO)
  }
  var locationLabel by remember(selectedMode) {
    mutableStateOf(
      when (selectedMode) {
        WorkMode.WFO -> "Kantor Pusat"
        WorkMode.WFH -> "Rumah / Remote"
        WorkMode.HYBRID -> "Lokasi Kunjungan"
      }
    )
  }
  var isDetectingLocation by remember { mutableStateOf(false) }
  var activityNote by remember(todayLog?.id, todayLog?.dailyActivities) {
    mutableStateOf(todayLog?.dailyActivities ?: "")
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      if (granted) {
        scope.launch {
          isDetectingLocation = true
          val detected = fetchCoarseLocationLabel(context)
          if (detected != null) {
            locationLabel = detected
          }
          isDetectingLocation = false
        }
      }
    }

  val internName = profile?.fullName?.ifBlank { displayNameFallback } ?: displayNameFallback
  val companyName = profile?.companyName?.ifBlank { "Perusahaan Mitra" } ?: "Perusahaan Mitra"
  val divisionName = profile?.division?.ifBlank { "Divisi Magang" } ?: "Divisi Magang"
  val shiftStart = profile?.shiftStart?.ifBlank { "07:00" } ?: "07:00"
  val shiftEnd = profile?.shiftEnd?.ifBlank { "16:00" } ?: "16:00"

  val monthPrefix = DateTimeFormatters.currentMonthPrefix(currentTimeMillis)
  val monthLogs = logs.filter { it.dateKey.startsWith(monthPrefix) }
  val onTimeCount = monthLogs.count { it.statusEnum == AttendanceStatus.ON_TIME }
  val lateCount = monthLogs.count { it.statusEnum == AttendanceStatus.LATE }
  val approvedLeaveCount =
    leaves.count { it.status == "APPROVED" && it.startDate.startsWith(monthPrefix) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 640.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Greeting Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Halo, $internName",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Apartment,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "$divisionName • $companyName",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        OutlinedButton(
          onClick = onOpenProfileEditor,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("home_edit_profile_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Profil",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Profil", style = MaterialTheme.typography.labelMedium)
        }
      }

      // First-time profile completion banner
      AnimatedVisibility(visible = profileState is UiState.Success && profile == null) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
          shape = RoundedCornerShape(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Lengkapi Profil Magang",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Text(
                text = "Atur divisi, nama pembimbing, dan jam shift Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
            TextButton(onClick = onOpenProfileEditor) {
              Text("Lengkapi")
            }
          }
        }
      }

      // Hero Live Clock & Attendance Status Card
      Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors =
                    listOf(
                      Color(0xFF1E3A8A),
                      Color(0xFF1E293B),
                    )
                )
              )
              .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = DateTimeFormatters.formatFullDate(currentTimeMillis),
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFCBD5E1),
            )
            Surface(
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = Color(0xFFFCD34D),
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "$shiftStart - $shiftEnd",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                )
              }
            }
          }

          Text(
            text = DateTimeFormatters.formatClockSeconds(currentTimeMillis),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
          )

          when {
            todayLog == null -> {
              StatusPill(
                text = "Belum Absen Hari Ini",
                containerColor = Color(0xFFF59E0B).copy(alpha = 0.22f),
                contentColor = Color(0xFFFDE68A),
                icon = Icons.Default.Warning,
              )
            }
            todayLog.stateEnum == SessionState.WORKING -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                StatusPill(
                  text = "Sedang Bekerja • ${todayLog.workModeEnum.label}",
                  containerColor = StatusWorkingColor.copy(alpha = 0.28f),
                  contentColor = Color(0xFFBFDBFE),
                  icon = Icons.Default.Schedule,
                )
                StatusPill(
                  text = todayLog.statusEnum.label,
                  containerColor =
                    if (todayLog.statusEnum == AttendanceStatus.ON_TIME)
                      StatusOnTimeColor.copy(alpha = 0.25f)
                    else StatusLateColor.copy(alpha = 0.28f),
                  contentColor = Color.White,
                )
              }
            }
            else -> {
              StatusPill(
                text = "Absensi Hari Ini Selesai",
                containerColor = StatusOnTimeColor.copy(alpha = 0.28f),
                contentColor = Color(0xFFA7F3D0),
                icon = Icons.Default.CheckCircle,
              )
            }
          }
        }
      }

      // Main Check-In / Check-Out Panel
      when (logsState) {
        is UiState.Loading -> {
          Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
              ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = logsState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          if (todayLog == null) {
            // Check-In Form Card
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = "Absen Masuk",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                  text = "Pilih Mode Kerja",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  WorkMode.entries.forEach { mode ->
                    FilterChip(
                      selected = selectedMode == mode,
                      onClick = { selectedMode = mode },
                      label = { Text("${mode.label} • ${mode.subtitle}") },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                      modifier = Modifier.testTag("work_mode_${mode.name.lowercase()}"),
                    )
                  }
                }

                OutlinedTextField(
                  value = locationLabel,
                  onValueChange = { locationLabel = it.take(120) },
                  label = { Text("Lokasi Kerja") },
                  leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                  },
                  trailingIcon = {
                    TextButton(
                      onClick = {
                        val hasPerm =
                          ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                          ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                          scope.launch {
                            isDetectingLocation = true
                            val detected = fetchCoarseLocationLabel(context)
                            if (detected != null) locationLabel = detected
                            isDetectingLocation = false
                          }
                        } else {
                          locationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                          )
                        }
                      },
                      enabled = !isDetectingLocation,
                    ) {
                      if (isDetectingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                      } else {
                        Icon(
                          imageVector = Icons.Default.MyLocation,
                          contentDescription = "Deteksi GPS",
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("location_input"),
                )

                OutlinedTextField(
                  value = activityNote,
                  onValueChange = { activityNote = it.take(500) },
                  label = { Text("Rencana Aktivitas Hari Ini (Opsional)") },
                  placeholder = { Text("Contoh: Mengerjakan integrasi modul API dan daily standup") },
                  minLines = 2,
                  maxLines = 4,
                  modifier = Modifier.fillMaxWidth().testTag("daily_activity_input"),
                )

                Button(
                  onClick = {
                    onCheckIn(selectedMode, locationLabel, activityNote)
                  },
                  enabled = !isSubmitting,
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(52.dp)
                      .testTag("check_in_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  if (isSubmitting) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Mencatat Absensi...")
                  } else {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Login,
                      contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Absen Masuk Sekarang",
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                }
              }
            }
          } else {
            // Working or Completed Card
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text(
                      text = "Jam Masuk",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = DateTimeFormatters.formatTimeHHmm(todayLog.checkInTime),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = "Durasi",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = DateTimeFormatters.formatDuration(todayLog.checkInTime, todayLog.checkOutTime),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "Jam Pulang",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = DateTimeFormatters.formatTimeHHmm(todayLog.checkOutTime),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }

                if (todayLog.locationLabel.isNotBlank()) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = "${todayLog.workModeEnum.label} • ${todayLog.locationLabel}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                OutlinedTextField(
                  value = activityNote,
                  onValueChange = { activityNote = it.take(500) },
                  label = { Text("Logbook Aktivitas Hari Ini") },
                  placeholder = { Text("Tulis pekerjaan yang sudah diselesaikan hari ini...") },
                  minLines = 3,
                  maxLines = 5,
                  modifier = Modifier.fillMaxWidth().testTag("logbook_note_input"),
                )

                if (todayLog.stateEnum == SessionState.WORKING) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    OutlinedButton(
                      onClick = { onUpdateActivities(todayLog.id, activityNote) },
                      enabled = !isSubmitting,
                      modifier = Modifier.weight(1f).height(50.dp).testTag("save_logbook_button"),
                      shape = RoundedCornerShape(14.dp),
                    ) {
                      Text("Simpan Log")
                    }
                    Button(
                      onClick = { onCheckOut(todayLog.id, activityNote) },
                      enabled = !isSubmitting,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondary,
                          contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                      modifier = Modifier.weight(1.3f).height(50.dp).testTag("check_out_button"),
                      shape = RoundedCornerShape(14.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Absen Pulang")
                    }
                  }
                } else {
                  OutlinedButton(
                    onClick = { onUpdateActivities(todayLog.id, activityNote) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("update_completed_logbook_button"),
                    shape = RoundedCornerShape(14.dp),
                  ) {
                    Text("Perbarui Catatan Logbook")
                  }
                }
              }
            }
          }
        }
      }

      // Monthly Summary Strip
      Text(
        text = "Ringkasan Bulan Ini",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryStatCard(
          title = "Total Hadir",
          value = monthLogs.size.toString(),
          accentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        SummaryStatCard(
          title = "Tepat Waktu",
          value = onTimeCount.toString(),
          accentColor = StatusOnTimeColor,
          modifier = Modifier.weight(1f),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryStatCard(
          title = "Terlambat",
          value = lateCount.toString(),
          accentColor = StatusLateColor,
          modifier = Modifier.weight(1f),
        )
        SummaryStatCard(
          title = "Izin Disetujui",
          value = approvedLeaveCount.toString(),
          accentColor = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCoarseLocationLabel(context: Context): String? {
  return try {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val location =
      fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        ?: fusedClient.lastLocation.await()
        ?: return null

    withContext(Dispatchers.IO) {
      try {
        val geocoder = Geocoder(context, Locale("id", "ID"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val latLng = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
          @Suppress("DEPRECATION")
          val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
          val area = addresses?.firstOrNull()?.subAdminArea ?: addresses?.firstOrNull()?.locality
          if (!area.isNullOrBlank()) "$area ($latLng)" else "GPS ($latLng)"
        } else {
          @Suppress("DEPRECATION")
          val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
          val area = addresses?.firstOrNull()?.subAdminArea ?: addresses?.firstOrNull()?.locality
          val latLng = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
          if (!area.isNullOrBlank()) "$area ($latLng)" else "GPS ($latLng)"
        }
      } catch (_: Exception) {
        String.format(Locale.US, "GPS %.4f, %.4f", location.latitude, location.longitude)
      }
    }
  } catch (_: Exception) {
    null
  }
}
