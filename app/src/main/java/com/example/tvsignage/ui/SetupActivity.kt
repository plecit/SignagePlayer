package com.example.tvsignage.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tvsignage.R
import com.example.tvsignage.SignageApplication
import com.example.tvsignage.core.HardwareKeyManager
import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.databinding.ActivitySetupBinding
import com.example.tvsignage.xmds.XmdsClient
import com.example.tvsignage.xmds.models.ActivationStatus
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var securePrefs: SecurePreferences
    private lateinit var xmdsClient: XmdsClient
    private lateinit var hardwareKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SignageApplication
        securePrefs = app.securePreferences
        xmdsClient = XmdsClient(securePrefs)
        hardwareKey = HardwareKeyManager.getHardwareKey(this)

        populateFields()
        setupListeners()
    }

    private fun populateFields() {
        binding.etCmsUrl.setText(securePrefs.cmsUrl)
        binding.etServerKey.setText(securePrefs.serverKey)
        binding.etDisplayName.setText(securePrefs.displayName)
        binding.tvHardwareKey.text = hardwareKey
    }

    private fun setupListeners() {
        binding.btnConnect.setOnClickListener {
            val url = binding.etCmsUrl.text.toString().trim()
            val key = binding.etServerKey.text.toString().trim()
            val name = binding.etDisplayName.text.toString().trim()

            if (url.isBlank()) {
                binding.etCmsUrl.error = "CMS URL required"
                binding.etCmsUrl.requestFocus()
                return@setOnClickListener
            }
            if (key.isBlank()) {
                binding.etServerKey.error = "Server Key required"
                binding.etServerKey.requestFocus()
                return@setOnClickListener
            }

            // Save entries
            securePrefs.cmsUrl = url
            securePrefs.serverKey = key
            securePrefs.displayName = if (name.isNotBlank()) name else "Android-TV-Player"

            testConnectionAndRegister()
        }
    }

    private fun testConnectionAndRegister() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.status_connecting)
        binding.btnConnect.isEnabled = false

        lifecycleScope.launch {
            try {
                SystemLogger.i(TAG, "Testing connection to ${securePrefs.cmsUrl}...")
                val result = xmdsClient.registerDisplay(hardwareKey)
                binding.progressBar.visibility = View.GONE
                binding.btnConnect.isEnabled = true

                when (result.status) {
                    ActivationStatus.READY -> {
                        securePrefs.isAuthorised = true
                        binding.tvStatus.text = getString(R.string.status_registered)
                        Toast.makeText(this@SetupActivity, R.string.status_registered, Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    ActivationStatus.WAITING -> {
                        securePrefs.isAuthorised = false
                        binding.tvStatus.text = getString(R.string.status_waiting_auth)
                        Toast.makeText(this@SetupActivity, R.string.status_waiting_auth, Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    ActivationStatus.DISABLED -> {
                        binding.tvStatus.text = "Display is blocked or disabled in CMS."
                    }
                    ActivationStatus.UNKNOWN -> {
                        binding.tvStatus.text = "Received response from CMS: ${result.message}"
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnConnect.isEnabled = true
                SystemLogger.e(TAG, "Registration connection failed: ${e.message}", e)
                binding.tvStatus.text = "Connection failed: ${e.message}"
            }
        }
    }

    companion object {
        private const val TAG = "SetupActivity"
    }
}
