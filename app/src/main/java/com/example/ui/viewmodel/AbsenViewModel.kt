package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceStatus
import com.example.data.model.InternProfile
import com.example.data.model.LeaveRequest
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.UiState
import com.example.data.model.WorkMode
import com.example.data.repository.AttendanceRepository
import com.example.util.DateTimeFormatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyAttendanceSummary(
  val totalPresent: Int = 0,
  val onTimeCount: Int = 0,
  val lateCount: Int = 0,
  val approvedLeaveCount: Int = 0,
)

class AbsenViewModel(
  private val repository: AttendanceRepository,
  val currentUserId: String,
  val currentUserEmail: String,
  val currentUserDisplayName: String,
  isEmailVerified: Boolean,
) : ViewModel() {

  val isAdmin: Boolean =
    currentUserEmail.equals("perfectichsan@gmail.com", ignoreCase = true) && isEmailVerified

  val currentTimeMillis: StateFlow<Long> =
    flow {
        while (true) {
          emit(System.currentTimeMillis())
          delay(1000L)
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = System.currentTimeMillis(),
      )

  val profileState: StateFlow<UiState<InternProfile?>> =
    repository
      .observeProfile(currentUserId)
      .map<InternProfile?, UiState<InternProfile?>> { UiState.Success(it) }
      .catch { error -> emit(unwrapFlowError(error, "Gagal memuat profil magang")) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val attendanceLogsState: StateFlow<UiState<List<AttendanceLog>>> =
    repository
      .observeAttendanceLogs(currentUserId)
      .map<List<AttendanceLog>, UiState<List<AttendanceLog>>> { UiState.Success(it) }
      .catch { error -> emit(unwrapFlowError(error, "Gagal memuat riwayat absensi")) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val leaveRequestsState: StateFlow<UiState<List<LeaveRequest>>> =
    repository
      .observeLeaveRequests(currentUserId)
      .map<List<LeaveRequest>, UiState<List<LeaveRequest>>> { UiState.Success(it) }
      .catch { error -> emit(unwrapFlowError(error, "Gagal memuat daftar pengajuan izin")) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminAttendanceState: StateFlow<UiState<List<AttendanceLog>>> =
    if (isAdmin) {
      repository
        .observeAllAttendanceLogsForAdmin(currentUserId)
        .map<List<AttendanceLog>, UiState<List<AttendanceLog>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFlowError(error, "Gagal memuat absensi seluruh peserta")) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = UiState.Loading,
        )
    } else {
      MutableStateFlow<UiState<List<AttendanceLog>>>(UiState.Success(emptyList())).asStateFlow()
    }

  val adminLeaveState: StateFlow<UiState<List<LeaveRequest>>> =
    if (isAdmin) {
      repository
        .observeAllLeaveRequestsForAdmin(currentUserId)
        .map<List<LeaveRequest>, UiState<List<LeaveRequest>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFlowError(error, "Gagal memuat pengajuan izin peserta")) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = UiState.Loading,
        )
    } else {
      MutableStateFlow<UiState<List<LeaveRequest>>>(UiState.Success(emptyList())).asStateFlow()
    }

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun computeMonthlySummary(
    logs: List<AttendanceLog>,
    leaves: List<LeaveRequest>,
  ): MonthlyAttendanceSummary {
    val monthPrefix = DateTimeFormatters.currentMonthPrefix()
    val monthLogs = logs.filter { it.dateKey.startsWith(monthPrefix) }
    val onTime = monthLogs.count { it.statusEnum == AttendanceStatus.ON_TIME }
    val late = monthLogs.count { it.statusEnum == AttendanceStatus.LATE }
    val approvedLeaves =
      leaves.count {
        it.statusEnum == LeaveStatus.APPROVED && it.startDate.startsWith(monthPrefix)
      }
    return MonthlyAttendanceSummary(
      totalPresent = monthLogs.size,
      onTimeCount = onTime,
      lateCount = late,
      approvedLeaveCount = approvedLeaves,
    )
  }

  fun saveProfile(
    fullName: String,
    division: String,
    university: String,
    mentorName: String,
    companyName: String,
    workModeDefault: WorkMode,
    shiftStart: String,
    shiftEnd: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val result =
        repository.saveProfile(
          fullName = fullName,
          email = currentUserEmail.ifBlank { "magang@perusahaan.id" },
          division = division,
          university = university,
          mentorName = mentorName,
          companyName = companyName,
          workModeDefault = workModeDefault,
          shiftStart = shiftStart,
          shiftEnd = shiftEnd,
        )
      _isSubmitting.value = false
      result
        .onSuccess {
          _feedbackMessage.value = "Profil magang berhasil disimpan."
          onSuccess()
        }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal menyimpan profil."
        }
    }
  }

  fun checkIn(
    profile: InternProfile?,
    workMode: WorkMode,
    locationLabel: String,
    dailyActivities: String,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val internName =
        profile?.fullName?.takeIf { it.isNotBlank() }
          ?: currentUserDisplayName.ifBlank { "Peserta Magang" }
      val division = profile?.division?.takeIf { it.isNotBlank() } ?: "Magang"
      val shiftStart = profile?.shiftStart?.takeIf { it.isNotBlank() } ?: "07:00"

      val result =
        repository.checkIn(
          internName = internName,
          division = division,
          shiftStart = shiftStart,
          workMode = workMode,
          locationLabel = locationLabel,
          dailyActivities = dailyActivities,
        )
      _isSubmitting.value = false
      result
        .onSuccess { _feedbackMessage.value = "Absen masuk berhasil dicatat!" }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal mencatat absen masuk."
        }
    }
  }

  fun checkOut(logId: String, dailyActivities: String) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val result = repository.checkOut(logId = logId, dailyActivities = dailyActivities)
      _isSubmitting.value = false
      result
        .onSuccess { _feedbackMessage.value = "Absen pulang berhasil dicatat. Terima kasih!" }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal mencatat absen pulang."
        }
    }
  }

  fun updateDailyActivities(
    logId: String,
    dailyActivities: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val result = repository.updateDailyActivities(logId, dailyActivities)
      _isSubmitting.value = false
      result
        .onSuccess {
          _feedbackMessage.value = "Logbook aktivitas harian diperbarui."
          onSuccess()
        }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal memperbarui logbook."
        }
    }
  }

  fun submitLeaveRequest(
    profile: InternProfile?,
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    reason: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val internName =
        profile?.fullName?.takeIf { it.isNotBlank() }
          ?: currentUserDisplayName.ifBlank { "Peserta Magang" }
      val division = profile?.division?.takeIf { it.isNotBlank() } ?: "Umum"

      val result =
        repository.submitLeaveRequest(
          internName = internName,
          division = division,
          leaveType = leaveType,
          startDate = startDate,
          endDate = endDate,
          reason = reason,
        )
      _isSubmitting.value = false
      result
        .onSuccess {
          _feedbackMessage.value = "Pengajuan izin berhasil dikirim."
          onSuccess()
        }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal mengirim pengajuan izin."
        }
    }
  }

  fun cancelLeaveRequest(requestId: String) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val result = repository.cancelLeaveRequest(requestId)
      _isSubmitting.value = false
      result
        .onSuccess { _feedbackMessage.value = "Pengajuan izin dibatalkan." }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal membatalkan pengajuan."
        }
    }
  }

  fun reviewLeaveRequest(
    requestId: String,
    newStatus: LeaveStatus,
    reviewerNote: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val result = repository.reviewLeaveRequest(requestId, newStatus, reviewerNote)
      _isSubmitting.value = false
      result
        .onSuccess {
          val statusText = if (newStatus == LeaveStatus.APPROVED) "disetujui" else "ditolak"
          _feedbackMessage.value = "Pengajuan berhasil $statusText."
          onSuccess()
        }
        .onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Gagal memproses persetujuan."
        }
    }
  }

  private fun unwrapFlowError(error: Throwable, fallbackMessage: String): UiState.Error {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, fallbackMessage, rootError)
    return UiState.Error(rootError.message ?: fallbackMessage)
  }

  private companion object {
    const val TAG = "AbsenViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
