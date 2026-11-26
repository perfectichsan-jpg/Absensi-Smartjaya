package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.InternProfile
import com.example.data.model.UiState
import com.example.ui.components.StatusPill

@Composable
fun ProfileScreen(
  profileState: UiState<InternProfile?>,
  userEmail: String,
  userDisplayName: String,
  isAdmin: Boolean,
  onEditProfile: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val profile = (profileState as? UiState.Success)?.data
  val fullName = profile?.fullName?.ifBlank { userDisplayName } ?: userDisplayName.ifBlank { "Peserta Magang" }
  val division = profile?.division?.ifBlank { "Belum diatur" } ?: "Belum diatur"
  val company = profile?.companyName?.ifBlank { "Perusahaan Mitra" } ?: "Perusahaan Mitra"
  val university = profile?.university?.ifBlank { "-" } ?: "-"
  val mentor = profile?.mentorName?.ifBlank { "-" } ?: "-"
  val shift =
    if (profile != null) "${profile.shiftStart} – ${profile.shiftEnd} WIB (${profile.workModeEnum.label})"
    else "07:00 – 16:00 WIB (WFO)"

  val initials =
    fullName
      .trim()
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifEmpty { "AM" }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Kartu Tanda Magang",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )

      // Digital Intern ID Card
      Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F766E))
                )
              )
              .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Column {
                Text(
                  text = fullName,
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = division,
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFD1FAE5),
                )
              }
            }
            if (isAdmin) {
              StatusPill(
                text = "Pembimbing",
                containerColor = Color(0xFFFCD34D),
                contentColor = Color(0xFF451A03),
              )
            }
          }

          HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Perusahaan",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
              )
              Text(
                text = company,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Status",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
              )
              Text(
                text = "Peserta Magang Aktif",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA7F3D0),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      // Details Card
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
          ProfileInfoRow(Icons.Default.Email, "Email Google", userEmail.ifBlank { "-" })
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          ProfileInfoRow(Icons.Default.Apartment, "Perusahaan", company)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          ProfileInfoRow(Icons.Default.Badge, "Divisi", division)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          ProfileInfoRow(Icons.Default.School, "Asal Kampus / Sekolah", university)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          ProfileInfoRow(Icons.Default.SupervisorAccount, "Pembimbing Lapangan", mentor)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          ProfileInfoRow(Icons.Default.Schedule, "Jadwal Kerja Target", shift)
        }
      }

      Button(
        onClick = onEditProfile,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_edit_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ubah Profil Magang")
      }

      OutlinedButton(
        onClick = onSignOut,
        colors =
          ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
          ),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Keluar Akun")
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun ProfileInfoRow(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
