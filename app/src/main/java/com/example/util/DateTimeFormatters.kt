package com.example.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeFormatters {
  private val localeId = Locale("id", "ID")

  fun formatFullDate(millis: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", localeId)
    return sdf.format(Date(millis))
  }

  fun formatShortDate(dateKey: String): String {
    return try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val parsed = parser.parse(dateKey) ?: return dateKey
      val out = SimpleDateFormat("EEE, dd MMM yyyy", localeId)
      out.format(parsed)
    } catch (_: Exception) {
      dateKey
    }
  }

  fun formatClockSeconds(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
    return sdf.format(Date(millis))
  }

  fun formatTimeHHmm(timestamp: Timestamp?): String {
    if (timestamp == null) return "--:--"
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    return "${sdf.format(timestamp.toDate())} WIB"
  }

  fun currentDateKey(millis: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date(millis))
  }

  fun currentMonthPrefix(millis: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    return sdf.format(Date(millis))
  }

  fun formatDuration(checkIn: Timestamp?, checkOut: Timestamp?): String {
    if (checkIn == null) return "-"
    val endMillis = checkOut?.toDate()?.time ?: System.currentTimeMillis()
    val diffMinutes = ((endMillis - checkIn.toDate().time) / 60_000L).coerceAtLeast(0L)
    val hours = diffMinutes / 60
    val minutes = diffMinutes % 60
    return "${hours}j ${minutes.toString().padStart(2, '0')}m"
  }

  fun isLateNow(shiftStart: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val parts = shiftStart.split(":")
    if (parts.size != 2) return false
    val shiftHour = parts[0].toIntOrNull() ?: return false
    val shiftMin = parts[1].toIntOrNull() ?: return false
    val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(nowMillis)).split(":")
    val nowHour = nowTime[0].toIntOrNull() ?: return false
    val nowMin = nowTime[1].toIntOrNull() ?: return false
    return (nowHour * 60 + nowMin) > (shiftHour * 60 + shiftMin)
  }
}
