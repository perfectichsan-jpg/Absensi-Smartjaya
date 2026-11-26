package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceLog
import com.example.data.model.AttendanceStatus
import com.example.data.model.InternProfile
import com.example.data.model.LeaveRequest
import com.example.data.model.LeaveStatus
import com.example.data.model.LeaveType
import com.example.data.model.SessionState
import com.example.data.model.WorkMode
import com.example.util.DateTimeFormatters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AttendanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan akun Google terlebih dahulu.")
  }

  fun observeProfile(userId: String): Flow<InternProfile?> = flow {
    require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              InternProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(userId, OperationType.GET, path, error)
          throw error
        }
    )
  }

  fun observeAttendanceLogs(userId: String): Flow<List<AttendanceLog>> = flow {
    require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
    val path = "attendance_logs"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AttendanceLog::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.dateKey }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(userId, OperationType.LIST, path, error)
          throw error
        }
    )
  }

  fun observeLeaveRequests(userId: String): Flow<List<LeaveRequest>> = flow {
    require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
    val path = "leave_requests"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              LeaveRequest::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(userId, OperationType.LIST, path, error)
          throw error
        }
    )
  }

  fun observeAllAttendanceLogsForAdmin(adminUserId: String): Flow<List<AttendanceLog>> = flow {
    require(adminUserId.isNotBlank()) { "Admin User ID tidak boleh kosong" }
    val path = "attendance_logs"
    emitAll(
      db.collection(path)
        .limit(100)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AttendanceLog::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedWith(
              compareByDescending<AttendanceLog> { it.dateKey }
                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(adminUserId, OperationType.LIST, path, error)
          throw error
        }
    )
  }

  fun observeAllLeaveRequestsForAdmin(adminUserId: String): Flow<List<LeaveRequest>> = flow {
    require(adminUserId.isNotBlank()) { "Admin User ID tidak boleh kosong" }
    val path = "leave_requests"
    emitAll(
      db.collection(path)
        .limit(100)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              LeaveRequest::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(adminUserId, OperationType.LIST, path, error)
          throw error
        }
    )
  }

  suspend fun saveProfile(
    fullName: String,
    email: String,
    division: String,
    university: String,
    mentorName: String,
    companyName: String,
    workModeDefault: WorkMode,
    shiftStart: String,
    shiftEnd: String,
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
    return try {
      val uid = requireUserId()
      val cleanName = fullName.trim().take(80)
      val cleanEmail = email.trim().take(120)
      val cleanDivision = division.trim().take(60)
      val cleanUniversity = university.trim().take(100)
      val cleanMentor = mentorName.trim().take(80)
      val cleanCompany = companyName.trim().take(80)
      val timeRegex = Regex("^[0-2][0-9]:[0-5][0-9]$")

      require(cleanName.isNotEmpty()) { "Nama lengkap wajib diisi." }
      require(cleanEmail.length >= 3) { "Email akun tidak valid." }
      require(cleanDivision.isNotEmpty()) { "Divisi magang wajib diisi." }
      require(cleanCompany.isNotEmpty()) { "Nama perusahaan wajib diisi." }
      require(timeRegex.matches(shiftStart)) { "Format jam masuk harus HH:mm (contoh 07:00)." }
      require(timeRegex.matches(shiftEnd)) { "Format jam pulang harus HH:mm (contoh 16:00)." }

      val docRef = db.collection("users").document(uid)
      docPath = docRef.path
      val exists = docRef.get().await().exists()

      if (exists) {
        docRef
          .update(
            mapOf(
              "fullName" to cleanName,
              "division" to cleanDivision,
              "university" to cleanUniversity,
              "mentorName" to cleanMentor,
              "companyName" to cleanCompany,
              "workModeDefault" to workModeDefault.name,
              "shiftStart" to shiftStart,
              "shiftEnd" to shiftEnd,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "fullName" to cleanName,
              "email" to cleanEmail,
              "division" to cleanDivision,
              "university" to cleanUniversity,
              "mentorName" to cleanMentor,
              "companyName" to cleanCompany,
              "workModeDefault" to workModeDefault.name,
              "shiftStart" to shiftStart,
              "shiftEnd" to shiftEnd,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun checkIn(
    internName: String,
    division: String,
    shiftStart: String,
    workMode: WorkMode,
    locationLabel: String,
    dailyActivities: String,
  ): Result<Unit> {
    val dateKey = DateTimeFormatters.currentDateKey()
    var docPath = "attendance_logs/pending"
    return try {
      val uid = requireUserId()
      val cleanName = internName.trim().ifEmpty { "Peserta Magang" }.take(80)
      val cleanDivision = division.trim().ifEmpty { "Umum" }.take(60)
      val cleanLocation = locationLabel.trim().take(120)
      val cleanActivities = dailyActivities.trim().take(500)
      val status =
        if (DateTimeFormatters.isLateNow(shiftStart)) AttendanceStatus.LATE
        else AttendanceStatus.ON_TIME

      val docId = "${uid}_$dateKey"
      val docRef = db.collection("attendance_logs").document(docId)
      docPath = docRef.path

      val payload =
        mapOf(
          "userId" to uid,
          "internName" to cleanName,
          "division" to cleanDivision,
          "dateKey" to dateKey,
          "workMode" to workMode.name,
          "attendanceStatus" to status.name,
          "checkInTime" to FieldValue.serverTimestamp(),
          "locationLabel" to cleanLocation,
          "dailyActivities" to cleanActivities,
          "sessionState" to SessionState.WORKING.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun checkOut(logId: String, dailyActivities: String): Result<Unit> {
    var docPath = "attendance_logs/$logId"
    return try {
      requireUserId()
      require(logId.isNotBlank()) { "ID absensi tidak valid." }
      val cleanActivities = dailyActivities.trim().take(500)
      val docRef = db.collection("attendance_logs").document(logId)
      docPath = docRef.path
      docRef
        .update(
          mapOf(
            "checkOutTime" to FieldValue.serverTimestamp(),
            "dailyActivities" to cleanActivities,
            "sessionState" to SessionState.COMPLETED.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateDailyActivities(logId: String, dailyActivities: String): Result<Unit> {
    var docPath = "attendance_logs/$logId"
    return try {
      requireUserId()
      require(logId.isNotBlank()) { "ID absensi tidak valid." }
      val cleanActivities = dailyActivities.trim().take(500)
      val docRef = db.collection("attendance_logs").document(logId)
      docPath = docRef.path
      docRef
        .update(
          mapOf(
            "dailyActivities" to cleanActivities,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun submitLeaveRequest(
    internName: String,
    division: String,
    leaveType: LeaveType,
    startDate: String,
    endDate: String,
    reason: String,
  ): Result<Unit> {
    var docPath = "leave_requests"
    return try {
      val uid = requireUserId()
      val cleanName = internName.trim().ifEmpty { "Peserta Magang" }.take(80)
      val cleanDivision = division.trim().ifEmpty { "Umum" }.take(60)
      val cleanReason = reason.trim().take(300)
      val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

      require(dateRegex.matches(startDate) && dateRegex.matches(endDate)) {
        "Format tanggal izin tidak valid."
      }
      require(endDate >= startDate) { "Tanggal selesai tidak boleh sebelum tanggal mulai." }
      require(cleanReason.length >= 5) { "Alasan izin minimal 5 karakter." }

      val docRef = db.collection("leave_requests").document()
      docPath = docRef.path
      docRef
        .set(
          mapOf(
            "userId" to uid,
            "internName" to cleanName,
            "division" to cleanDivision,
            "leaveType" to leaveType.name,
            "startDate" to startDate,
            "endDate" to endDate,
            "reason" to cleanReason,
            "status" to LeaveStatus.PENDING.name,
            "reviewerNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun cancelLeaveRequest(requestId: String): Result<Unit> {
    var docPath = "leave_requests/$requestId"
    return try {
      requireUserId()
      require(requestId.isNotBlank()) { "ID pengajuan tidak valid." }
      val docRef = db.collection("leave_requests").document(requestId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun reviewLeaveRequest(
    requestId: String,
    newStatus: LeaveStatus,
    reviewerNote: String,
  ): Result<Unit> {
    var docPath = "leave_requests/$requestId"
    return try {
      requireUserId()
      require(requestId.isNotBlank()) { "ID pengajuan tidak valid." }
      require(newStatus == LeaveStatus.APPROVED || newStatus == LeaveStatus.REJECTED) {
        "Status persetujuan harus Disetujui atau Ditolak."
      }
      val docRef = db.collection("leave_requests").document(requestId)
      docPath = docRef.path
      docRef
        .update(
          mapOf(
            "status" to newStatus.name,
            "reviewerNote" to reviewerNote.trim().take(200),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  private fun logFlowErrorIfSignedIn(
    userId: String,
    operationType: OperationType,
    path: String,
    error: Throwable,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}
