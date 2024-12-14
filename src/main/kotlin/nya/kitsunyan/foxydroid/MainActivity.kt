package nya.kitsunyan.foxydroid

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import nya.kitsunyan.foxydroid.screen.ScreenActivity

class MainActivity: ScreenActivity() {
  companion object {
    const val ACTION_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.action.UPDATES"
    const val ACTION_INSTALL = "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL"
    const val EXTRA_CACHE_FILE_NAME = "${BuildConfig.APPLICATION_ID}.intent.extra.CACHE_FILE_NAME"
  }

  override fun handleIntent(intent: Intent?) {
    when (intent?.action) {
      ACTION_UPDATES -> handleSpecialIntent(SpecialIntent.Updates)
      ACTION_INSTALL -> handleSpecialIntent(SpecialIntent.Install(intent.packageName,
        intent.getStringExtra(EXTRA_CACHE_FILE_NAME)))
      else -> super.handleIntent(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkNotificationPermission()
  }

  private var needNotifications = true

  private fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        needNotifications = true
        return
      }
      // do not ask again if user denied the request
      if (!needNotifications) return
      // always show a dialog to explain why we need notification permission,
      // regardless of `shouldShowRequestPermissionRationale(...)`
      AlertDialog.Builder(this)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setTitle(R.string.permissions)
        .setMessage(R.string.sync_repositories_automatically)
        .setNegativeButton(R.string.cancel) { _, _ ->
          // do not ask again if user denied the request
          needNotifications = false
        }
        .setPositiveButton(R.string.ok) { _, _ ->
          requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        .show()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != 0) return
    // do not ask again if user denied the request
    needNotifications = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
  }
}
