package com.example.tvsignage.ui

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tvsignage.R
import com.example.tvsignage.SignageApplication
import com.example.tvsignage.core.HardwareKeyManager
import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.databinding.ActivityPlayerBinding
import com.example.tvsignage.files.LocalStorageManager
import com.example.tvsignage.layout.Layout
import com.example.tvsignage.layout.XlfParser
import com.example.tvsignage.player.LayoutRenderer
import com.example.tvsignage.schedule.ScheduleManager
import com.example.tvsignage.service.PlayerForegroundService
import com.example.tvsignage.service.WatchdogManager
import com.example.tvsignage.stats.ProofOfPlayEngine
import com.example.tvsignage.xmds.XmdsClient
import com.example.tvsignage.xmds.models.ActivationStatus

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var securePrefs: SecurePreferences
    private lateinit var storageManager: LocalStorageManager
    private lateinit var layoutRenderer: LayoutRenderer
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var popEngine: ProofOfPlayEngine
    private lateinit var secretMenuTrigger: SecretMenuTrigger
    private lateinit var watchdog: WatchdogManager

    private var currentPlayingLayoutId: Long = 0L

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PlayerForegroundService.ACTION_STATUS_CHANGED -> {
                    val statusName = intent.getStringExtra(PlayerForegroundService.EXTRA_ACTIVATION_STATUS)
                    val status = ActivationStatus.values().find { it.name == statusName } ?: ActivationStatus.UNKNOWN
                    updateStatusDisplay(status)
                }
                PlayerForegroundService.ACTION_SCHEDULE_UPDATED -> {
                    evaluateAndPlayNextLayout()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullscreen()

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SignageApplication
        securePrefs = app.securePreferences
        storageManager = LocalStorageManager(this)
        val xmdsClient = XmdsClient(securePrefs)
        popEngine = ProofOfPlayEngine(app.statDatabase.statDao(), xmdsClient, securePrefs)
        layoutRenderer = LayoutRenderer(this, storageManager, popEngine)
        scheduleManager = ScheduleManager(storageManager)

        secretMenuTrigger = SecretMenuTrigger {
            showPinDialog()
        }

        watchdog = WatchdogManager(timeoutMs = 45_000L) {
            SystemLogger.w(TAG, "Watchdog triggered reload of layout")
            evaluateAndPlayNextLayout()
        }

        registerReceivers()

        if (!securePrefs.isConfigured()) {
            startActivity(Intent(this, SetupActivity::class.java))
        } else {
            startSignageService()
            if (!securePrefs.isAuthorised) {
                updateStatusDisplay(ActivationStatus.WAITING)
            } else {
                evaluateAndPlayNextLayout()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        makeFullscreen()
        watchdog.start()
        if (securePrefs.isConfigured()) {
            startSignageService()
            if (currentPlayingLayoutId == 0L) {
                evaluateAndPlayNextLayout()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        watchdog.stop()
        popEngine.onLayoutEnd()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(serviceReceiver)
        } catch (_: Exception) {}
        layoutRenderer.clear(binding.layoutContainer)
        super.onDestroy()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(PlayerForegroundService.ACTION_STATUS_CHANGED)
            addAction(PlayerForegroundService.ACTION_SCHEDULE_UPDATED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceReceiver, filter)
        }
    }

    private fun startSignageService() {
        val intent = Intent(this, PlayerForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateStatusDisplay(status: ActivationStatus) {
        val hardwareKey = HardwareKeyManager.getHardwareKey(this)
        when (status) {
            ActivationStatus.WAITING -> {
                binding.splashContainer.visibility = View.VISIBLE
                binding.splashSubtitle.text = getString(R.string.status_waiting_auth)
                binding.splashHardwareKey.text = hardwareKey
                binding.splashDesc.text = getString(R.string.status_waiting_auth_desc)
                binding.splashProgressBar.visibility = View.VISIBLE
            }
            ActivationStatus.READY -> {
                binding.splashContainer.visibility = View.GONE
            }
            ActivationStatus.DISABLED -> {
                binding.splashContainer.visibility = View.VISIBLE
                binding.splashSubtitle.text = "Display Disabled"
                binding.splashDesc.text = "This display has been disabled in Xibo CMS."
                binding.splashProgressBar.visibility = View.GONE
            }
            ActivationStatus.UNKNOWN -> {
                // Keep playing current cached layout
            }
        }
    }

    private fun evaluateAndPlayNextLayout() {
        watchdog.beat()
        val nextLayoutId = scheduleManager.getActiveLayout()

        if (nextLayoutId <= 0) {
            SystemLogger.i(TAG, "No active layout found in schedule or default layout.")
            if (currentPlayingLayoutId == 0L) {
                binding.splashContainer.visibility = View.VISIBLE
                binding.splashSubtitle.text = getString(R.string.status_no_layout)
                binding.splashHardwareKey.text = HardwareKeyManager.getHardwareKey(this)
                binding.splashProgressBar.visibility = View.VISIBLE
            }
            return
        }

        val layoutFile = storageManager.getFile("$nextLayoutId.xlf")
        if (!layoutFile.exists()) {
            SystemLogger.w(TAG, "Layout file does not exist on disk: ${layoutFile.path}")
            return
        }

        val parsedLayout = XlfParser.parse(layoutFile, nextLayoutId)
        if (parsedLayout == null) {
            SystemLogger.e(TAG, "Failed to parse XLF for layout $nextLayoutId")
            return
        }

        binding.splashContainer.visibility = View.GONE
        currentPlayingLayoutId = nextLayoutId

        SystemLogger.i(TAG, "Rendering layout ID $nextLayoutId (regions: ${parsedLayout.regions.size})")
        layoutRenderer.renderLayout(
            layout = parsedLayout,
            container = binding.layoutContainer,
            scheduleId = 1L,
            onLayoutCycleComplete = {
                evaluateAndPlayNextLayout()
            }
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (secretMenuTrigger.onKeyDown(keyCode, event)) {
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            confirmExit()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun showPinDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter Admin PIN (Default: 0000)"
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val entered = input.text.toString().trim()
                if (entered == securePrefs.adminPin) {
                    startActivity(Intent(this, SetupActivity::class.java))
                } else {
                    Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_exit_title)
            .setMessage(R.string.confirm_exit_desc)
            .setPositiveButton(R.string.btn_exit) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun makeFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    companion object {
        private const val TAG = "PlayerActivity"
    }
}
