package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class WorkMode(val label: String, val subtitle: String) {
  WFO("WFO", "Di Kantor"),
  WFH("WFH", "Kerja Jarak Jauh"),
  HYBRID("HYBRID", "Lapangan / Kunjungan");

  companion object {
    fun fromRaw(raw: String?): WorkMode =
      entries.firstOrNull { it.name == raw } ?: WFO
  }
}

enum class AttendanceStatus(val label: String) {
  ON_TIME("Tepat Waktu"),
  LATE("Terlambat");

  companion object {
    fun fromRaw(raw: String?): AttendanceStatus =
      entries.firstOrNull { it.name == raw } ?: ON_TIME
  }
}

enum class SessionState(val label: String) {
  WORKING("Sedang Bekerja"),
  COMPLETED("Selesai");

  companion object {
    fun fromRaw(raw: String?): SessionState =
      entries.firstOrNull { it.name == raw } ?: WORKING
  }
}

enum class LeaveType(val label: String) {
  SAKIT("Sakit"),
  IZIN_KAMPUS("Izin Kampus / Bimbingan"),
  PRIBADI("Keperluan Pribadi");

  companion object {
    fun fromRaw(raw: String?): LeaveType =
      entries.firstOrNull { it.name == raw } ?: IZIN_KAMPUS
  }
}

enum class LeaveStatus(val label: String) {
  PENDING("Menunggu"),
  APPROVED("Disetujui"),
  REJECTED("Ditolak");

  companion object {
    fun fromRaw(raw: String?): LeaveStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

@Keep
@IgnoreExtraProperties
data class InternProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val division: String = "Magang",
  val university: String = "",
  val mentorName: String = "",
  val companyName: String = "SmartJaya",
  val workModeDefault: String = WorkMode.WFO.name,
  val shiftStart: String = "07:00",
  val shiftEnd: String = "16:00",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val workModeEnum: WorkMode
    get() = WorkMode.fromRaw(workModeDefault)
}

@Keep
@IgnoreExtraProperties
data class AttendanceLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val internName: String = "",
  val division: String = "",
  val dateKey: String = "",
  val workMode: String = WorkMode.WFO.name,
  val attendanceStatus: String = AttendanceStatus.ON_TIME.name,
  @ServerTimestamp val checkInTime: Timestamp? = null,
  val checkOutTime: Timestamp? = null,
  val locationLabel: String = "",
  val dailyActivities: String = "",
  val sessionState: String = SessionState.WORKING.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val workModeEnum: WorkMode
    get() = WorkMode.fromRaw(workMode)

  val statusEnum: AttendanceStatus
    get() = AttendanceStatus.fromRaw(attendanceStatus)

  val stateEnum: SessionState
    get() = SessionState.fromRaw(sessionState)
}

@Keep
@IgnoreExtraProperties
data class LeaveRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val internName: String = "",
  val division: String = "",
  val leaveType: String = LeaveType.IZIN_KAMPUS.name,
  val startDate: String = "",
  val endDate: String = "",
  val reason: String = "",
  val status: String = LeaveStatus.PENDING.name,
  val reviewerNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val leaveTypeEnum: LeaveType
    get() = LeaveType.fromRaw(leaveType)

  val statusEnum: LeaveStatus
    get() = LeaveStatus.fromRaw(status)
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}
