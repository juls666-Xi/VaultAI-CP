package com.offlineai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.offlineai.navigation.AppNavGraph
import com.offlineai.ui.theme.OfflineAITheme
import com.offlineai.viewmodel.ChatViewModel

/**
 * MainActivity
 * ------------
 * Single-activity architecture — all navigation happens inside Compose.
 *
 * Responsibilities:
 *  1. Enable edge-to-edge display (modern Android UX).
 *  2. Create / retrieve the shared [ChatViewModel].
 *  3. Observe the dark-mode setting so the theme reacts in real time.
 *  4. Mount the root Compose content with [OfflineAITheme] + [AppNavGraph].
 *
 * USB note:
 *  When the user plugs in a USB device, Android may launch this Activity via
 *  the USB_DEVICE_ATTACHED intent (configured in AndroidManifest.xml).
 *  The [UsbDeviceManager] inside [ChatViewModel] picks that up automatically
 *  via its registered BroadcastReceiver.
 */
class MainActivity : ComponentActivity() {

    // viewModels() uses the ViewModelStore of this Activity, so the VM survives
    // configuration changes (rotation, dark-mode switch, etc.)
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: lets the app draw behind the status bar and nav bar.
        // ChatInputBar uses .navigationBarsPadding() to keep the input above the nav bar.
        enableEdgeToEdge()

        setContent {
            // Collect dark-mode preference as Compose state so the theme
            // recomposes automatically when the user toggles it in Settings.
            val settings by viewModel.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = com.offlineai.data.model.AppSettings())

            OfflineAITheme(darkTheme = settings.darkMode) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, viewModel = viewModel)
            }
        }
    }
}
