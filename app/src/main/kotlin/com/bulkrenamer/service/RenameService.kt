package com.bulkrenamer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.bulkrenamer.MainActivity
import com.bulkrenamer.RENAME_NOTIFICATION_CHANNEL_ID
import com.bulkrenamer.R
import com.bulkrenamer.domain.RenameFilesUseCase
import com.bulkrenamer.domain.RenameOperation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NOTIFICATION_ID = 1001

@AndroidEntryPoint
class RenameService : Service() {

    @Inject
    lateinit var renameFilesUseCase: RenameFilesUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    inner class RenameBinder : Binder() {
        val progress: StateFlow<RenameProgressState>
            get() = renameFilesUseCase.progress

        fun cancel() = renameFilesUseCase.cancel()
    }

    private val binder = RenameBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("DEPRECATION")
        val operations: List<RenameOperation> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableArrayListExtra("operations", RenameOperation::class.java) ?: emptyList()
        } else {
            intent?.getParcelableArrayListExtra("operations") ?: emptyList()
        }

        startForegroundCompat()

        serviceScope.launch {
            renameFilesUseCase.executeBatch(operations)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification("Starting rename...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )
    }

    fun updateNotification(text: String) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(contentText: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, RENAME_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Renaming files...")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
