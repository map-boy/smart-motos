# 02-Android-Fcm.ps1  -  push notifications: service, token, permission
# Part of the SmartMotos apply set. Safe to re-run: every step checks first.
$ErrorActionPreference = 'Stop'
if (-not (Test-Path .git)) { throw 'Run from the repo root.' }

function Save([string]$rel, [string]$text) {
  $full = Join-Path $PWD.Path $rel
  New-Item -ItemType Directory -Force -Path (Split-Path $full -Parent) | Out-Null
  [System.IO.File]::WriteAllText($full, $text)
  Write-Host "  wrote   $rel" -ForegroundColor DarkGreen
}

# Line endings are normalised before comparing: these files are CRLF in a Windows
# checkout but the anchors below are LF, and a raw Contains() would never match.
function Patch([string]$rel, [string]$find, [string]$replace, [string]$guard) {
  $full = Join-Path $PWD.Path $rel
  if (-not (Test-Path $full)) { Write-Host "  SKIP    $rel (not found)" -ForegroundColor Yellow; return }
  $raw  = [System.IO.File]::ReadAllText($full)
  $crlf = $raw.Contains("`r`n")
  $s    = $raw.Replace("`r`n", "`n")
  $f    = $find.Replace("`r`n", "`n")
  $r    = $replace.Replace("`r`n", "`n")
  if ($s.Contains($guard)) { Write-Host "  ok      $rel (already patched)" -ForegroundColor DarkGray; return }
  if (-not $s.Contains($f)) { throw "Anchor not found in $rel - the file differs from what this script expects." }
  $s = $s.Replace($f, $r)
  if ($crlf) { $s = $s.Replace("`n", "`r`n") }
  [System.IO.File]::WriteAllText($full, $s)
  Write-Host "  patched $rel" -ForegroundColor DarkGreen
}


Write-Host '[02] Android FCM' -ForegroundColor Cyan

Save 'android\app\src\main\java\com\nihonor\smartmotosapp\data\SmartMessagingService.kt' @'
package com.nihonor.smartmotosapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nihonor.smartmotosapp.MainActivity

// Android counterpart of SmartMessagingService.swift. functions/src/notifications.ts
// sends every push to users/{uid}.fcmToken, so a client that never writes that field
// silently receives nothing — which was the case on Android before this file existed.
class SmartMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        persistToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        ensureChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS may be denied on API 33+; notify() then no-ops rather than throwing.
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "smartmotos_trips"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Trip updates", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Ride offers, driver assignment, and trip status changes."
                }
            )
        }

        // Writes to the same users/{uid}.fcmToken field the iOS client and the
        // Cloud Functions both rely on. update() rather than set(): the profile doc
        // is created by ensureUserProfile, and a merge here could resurrect a
        // deleted user doc as a stub containing nothing but a token.
        fun persistToken(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    android.util.Log.e("SmartMessaging", "Failed to persist fcmToken", e)
                }
        }
    }
}
'@

# --- SmartRepository: import + token helpers ---
$rFind1 = @'
import com.google.firebase.functions.FirebaseFunctions
'@
$rRepl1 = @'
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\data\SmartRepository.kt' $rFind1 $rRepl1 'import com.google.firebase.messaging.FirebaseMessaging'

$rFind2 = @'
    // ---- mapping (mirrors SmartRepository.swift line-for-line) ----
'@
$rRepl2 = @'
    // onNewToken only fires when the token is first created or rotates. A user who
    // installs, gets a token, then signs in later would never hit that callback while
    // authenticated, so the token has to be fetched explicitly after login too.
    fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> SmartMessagingService.persistToken(token) }
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "Failed to fetch FCM token", e)
            }
    }

    // Clear the token on sign-out so pushes for this account stop landing on a device
    // that is no longer signed in to it.
    fun clearFcmToken(uid: String) {
        db.collection("users").document(uid)
            .update("fcmToken", FieldValue.delete())
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "Failed to clear fcmToken", e)
            }
    }

    // ---- mapping (mirrors SmartRepository.swift line-for-line) ----
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\data\SmartRepository.kt' $rFind2 $rRepl2 'fun registerFcmToken()'

# --- MainActivity: channel, runtime permission, token registration ---
$mFind1 = @'
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
'@
$mRepl1 = @'
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\MainActivity.kt' $mFind1 $mRepl1 'ActivityResultContracts'

$mFind2 = @'
import com.nihonor.smartmotosapp.data.SmartRepository
'@
$mRepl2 = @'
import com.nihonor.smartmotosapp.data.SmartMessagingService
import com.nihonor.smartmotosapp.data.SmartRepository
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\MainActivity.kt' $mFind2 $mRepl2 'data.SmartMessagingService'

$mFind3 = @'
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
'@
$mRepl3 = @'
    // Result is ignored on purpose: a declined prompt just means no notifications,
    // which must not block the app from starting.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SmartMessagingService.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\MainActivity.kt' $mFind3 $mRepl3 'notificationPermission'

$mFind4 = @'
                SmartRepository.attachListeners(uid!!)
'@
$mRepl4 = @'
                SmartRepository.attachListeners(uid!!)
                SmartRepository.registerFcmToken()
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\MainActivity.kt' $mFind4 $mRepl4 'SmartRepository.registerFcmToken()'

Write-Host '[02] done' -ForegroundColor Green
