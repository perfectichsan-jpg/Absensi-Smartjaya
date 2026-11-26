package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.InternProfile
import com.example.data.model.WorkMode

@Composable
fun StatusPill(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun SummaryStatCard(
  title: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = accentColor,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
      shape = CircleShape,
      modifier = Modifier.size(72.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(34.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorDialog(
  initialProfile: InternProfile?,
  defaultName: String,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    fullName: String,
    division: String,
    university: String,
    mentorName: String,
    companyName: String,
    workMode: WorkMode,
    shiftStart: String,
    shiftEnd: String,
  ) -> Unit,
) {
  var fullName by remember(initialProfile) {
    mutableStateOf(initialProfile?.fullName?.ifBlank { defaultName } ?: defaultName)
  }
  var division by remember(initialProfile) {
    mutableStateOf(initialProfile?.division?.ifBlank { "Magang" } ?: "Magang")
  }
  var university by remember(initialProfile) {
    mutableStateOf(initialProfile?.university ?: "")
  }
  var mentorName by remember(initialProfile) {
    mutableStateOf(initialProfile?.mentorName ?: "")
  }
  var companyName by remember(initialProfile) {
    mutableStateOf(initialProfile?.companyName?.ifBlank { "SmartJaya" } ?: "SmartJaya")
  }
  var workMode by remember(initialProfile) {
    mutableStateOf(initialProfile?.workModeEnum ?: WorkMode.WFO)
  }
  var shiftStart by remember(initialProfile) {
    mutableStateOf(initialProfile?.shiftStart?.ifBlank { "07:00" } ?: "07:00")
  }
  var shiftEnd by remember(initialProfile) {
    mutableStateOf(initialProfile?.shiftEnd?.ifBlank { "16:00" } ?: "16:00")
  }
  var validationError by remember { mutableStateOf<String?>(null) }

  val divisionPresets = listOf("Manager", "Marketing", "Admin", "Magang")
  val shiftPresets = listOf("07:00" to "16:00", "13:00" to "21:00")

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    title = {
      Text(
        text = if (initialProfile == null) "Lengkapi Profil Magang" else "Edit Profil Magang",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.widthIn(max = 460.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it.take(80) },
          label = { Text("Nama Lengkap") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
        )
        OutlinedTextField(
          value = companyName,
          onValueChange = { companyName = it.take(80) },
          label = { Text("Nama Perusahaan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_company_input"),
        )
        Text(
          text = "Divisi / Departemen",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          divisionPresets.forEach { preset ->
            FilterChip(
              selected = division == preset,
              onClick = { division = preset },
              label = { Text(preset) },
            )
          }
        }
        OutlinedTextField(
          value = division,
          onValueChange = { division = it.take(60) },
          label = { Text("Nama Divisi") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_division_input"),
        )
        OutlinedTextField(
          value = university,
          onValueChange = { university = it.take(100) },
          label = { Text("Asal Kampus / Sekolah") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_university_input"),
        )
        OutlinedTextField(
          value = mentorName,
          onValueChange = { mentorName = it.take(80) },
          label = { Text("Nama Pembimbing / Mentor") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_mentor_input"),
        )

        Text(
          text = "Mode Kerja Bawaan",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WorkMode.entries.forEach { mode ->
            FilterChip(
              selected = workMode == mode,
              onClick = { workMode = mode },
              label = { Text(mode.label) },
            )
          }
        }

        Text(
          text = "Jam Kerja Target",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          shiftPresets.forEach { (start, end) ->
            FilterChip(
              selected = shiftStart == start && shiftEnd == end,
              onClick = {
                shiftStart = start
                shiftEnd = end
              },
              label = { Text("${start.replace(':', '.')} - ${end.replace(':', '.')}") },
            )
          }
        }

        if (validationError != null) {
          Text(
            text = validationError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fullName.trim().isEmpty()) {
            validationError = "Nama lengkap wajib diisi."
            return@Button
          }
          if (companyName.trim().isEmpty()) {
            validationError = "Nama perusahaan wajib diisi."
            return@Button
          }
          if (division.trim().isEmpty()) {
            validationError = "Divisi magang wajib diisi."
            return@Button
          }
          validationError = null
          onSave(
            fullName,
            division,
            university,
            mentorName,
            companyName,
            workMode,
            shiftStart,
            shiftEnd,
          )
        },
        enabled = !isSubmitting,
        modifier = Modifier.testTag("save_profile_button"),
      ) {
        Text(if (isSubmitting) "Menyimpan..." else "Simpan")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSubmitting,
      ) {
        Text("Batal")
      }
    },
  )
}
