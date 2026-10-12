package io.github.ngth1010v.mydailynote

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import io.github.ngth1010v.mydailynote.core.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

private fun launchIntent(context: Context, action: String) =
    Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun launchPending(context: Context, action: String, requestCode: Int) =
    PendingIntent.getActivity(
        context, requestCode, launchIntent(context, action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

/** Home-screen widget: one tap to add by voice or by text. */
class QuickAddWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
            setOnClickPendingIntent(R.id.widget_voice, launchPending(context, MainActivity.ACTION_ADD_VOICE, 1))
            setOnClickPendingIntent(R.id.widget_text, launchPending(context, MainActivity.ACTION_ADD_TEXT, 2))
            setOnClickPendingIntent(R.id.widget_title, launchPending(context, Intent.ACTION_MAIN, 3))
        }
        manager.updateAppWidget(ids, views)
    }
}

/** Quick Settings tile that opens straight into voice capture. */
class VoiceTileService : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(launchPending(this, MainActivity.ACTION_ADD_VOICE, 4))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent(this, MainActivity.ACTION_ADD_VOICE))
        }
    }
}

/**
 * Android device backup is opt-in (Settings > Data > Android backup). Files included are listed in
 * res/xml/backup_rules.xml and data_extraction_rules.xml; downloaded models are never backed up.
 */
class AppBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        val enabled = runCatching { runBlocking { SettingsRepository(this@AppBackupAgent).current().autoBackup } }
            .getOrDefault(false)
        if (enabled) super.onFullBackup(data)
    }

    // Key/value backup is not used (fullBackupOnly="true").
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {}
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) {}
}
