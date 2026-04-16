package com.offlineai.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.offlineai.data.model.UsbConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UsbDeviceManager
 * ----------------
 * Manages the full USB Host API lifecycle:
 *
 *  1. Enumerate connected USB devices via [UsbManager].
 *  2. Request user permission to access the device.
 *  3. Detect attach/detach events via a BroadcastReceiver.
 *  4. Expose connection state as a [StateFlow] for the ViewModel to observe.
 *
 * ──────────────────────────────────────────────────────────────
 * HOW ANDROID USB HOST API WORKS (high level):
 * ──────────────────────────────────────────────────────────────
 *
 *  Android USB Host mode (OTG) lets an Android device act as the USB "host",
 *  meaning it can power and communicate with attached USB peripherals
 *  (keyboards, flash drives, custom hardware, etc.).
 *
 *  Key classes:
 *   • UsbManager  – system service; enumerates devices, grants permissions.
 *   • UsbDevice   – represents a connected USB device; has vendorId, productId,
 *                   device class, and a list of UsbInterface objects.
 *   • UsbInterface – a logical function of the device (e.g., mass-storage interface).
 *   • UsbEndpoint – a communication channel on an interface (bulk IN / bulk OUT).
 *   • UsbDeviceConnection – an open connection; used to claim interfaces and
 *                           issue bulk transfers (bulkTransfer()).
 *
 *  Typical flow:
 *   1. User plugs in USB device.
 *   2. Android fires USB_DEVICE_ATTACHED (configured in AndroidManifest.xml).
 *   3. App calls usbManager.requestPermission() — system shows dialog.
 *   4. USB_DEVICE_ACTION broadcast returns with EXTRA_PERMISSION_GRANTED.
 *   5. App calls usbManager.openDevice() to get UsbDeviceConnection.
 *   6. App claims the mass-storage interface and issues bulk transfers.
 *
 *  For reading model files from a USB flash drive, the simplest real-world
 *  approach is to use Android's StorageManager / MediaStore to read the
 *  mounted volume as a file path (e.g., /storage/XXXX-XXXX/model.gguf)
 *  rather than raw bulk USB transfers, which require re-implementing the
 *  FAT32 filesystem in the app.
 */
class UsbDeviceManager(private val context: Context) {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.offlineai.USB_PERMISSION"

        /** Common model file extensions to scan for on the USB drive. */
        private val MODEL_EXTENSIONS = listOf(".gguf", ".bin", ".pt", ".onnx", ".tflite")
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)

    /** Observe this in ViewModel to react to USB connect/disconnect/error events. */
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    // ──────────────────────────────────────────────────────────────
    // BroadcastReceiver: listens for permission results + detach
    // ──────────────────────────────────────────────────────────────

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> handlePermissionResult(intent)
                UsbManager.ACTION_USB_DEVICE_DETACHED -> handleDeviceDetached(intent)
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> handleDeviceAttached(intent)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Lifecycle: register / unregister the receiver
    // ──────────────────────────────────────────────────────────────

    /** Call from Activity.onStart() or Application.onCreate(). */
    fun register() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(usbReceiver, filter)
        }
    }

    /** Call from Activity.onStop() or when the manager is no longer needed. */
    fun unregister() {
        try { context.unregisterReceiver(usbReceiver) } catch (_: Exception) { }
    }

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────

    /**
     * Scan for already-connected USB devices and request permission
     * for the first mass-storage device found.
     *
     * Call this from "Connect to Offline AI" button or when the app starts.
     */
    fun connectToUsbDevice() {
        _connectionState.value = UsbConnectionState.Connecting

        val deviceList: Map<String, UsbDevice> = usbManager.deviceList

        if (deviceList.isEmpty()) {
            _connectionState.value = UsbConnectionState.Error(
                "No USB device detected. Please plug in your USB drive."
            )
            return
        }

        // Pick the first available device (in a real app you might show a picker)
        val device = deviceList.values.first()
        requestPermission(device)
    }

    /**
     * Manually disconnect and reset connection state.
     */
    fun disconnect() {
        _connectionState.value = UsbConnectionState.Disconnected
    }

    /**
     * Return the mounted file-system path for an attached USB storage volume.
     *
     * Android mounts USB OTG drives under /storage/<VOLUME_ID>/.
     * We scan that directory for known model file extensions.
     */
    fun findModelFilesOnUsb(): List<String> {
        val storageDir = java.io.File("/storage")
        if (!storageDir.exists()) return emptyList()

        val modelFiles = mutableListOf<String>()

        // Walk top-level storage volumes (skips "emulated" = internal storage)
        storageDir.listFiles()
            ?.filter { it.name != "emulated" && it.name != "self" }
            ?.forEach { volume ->
                volume.walkTopDown()
                    .filter { file ->
                        file.isFile && MODEL_EXTENSIONS.any { ext ->
                            file.name.endsWith(ext, ignoreCase = true)
                        }
                    }
                    .forEach { modelFiles.add(it.absolutePath) }
            }

        return modelFiles
    }

    // ──────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────

    private fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            // Permission already granted (device was used before)
            onPermissionGranted(device)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE else 0

            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_PERMISSION),
                flags
            )
            // Shows system dialog: "Allow <App> to access <Device>?"
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun handlePermissionResult(intent: Intent) {
        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

        if (granted && device != null) {
            onPermissionGranted(device)
        } else {
            _connectionState.value = UsbConnectionState.Error(
                "USB permission denied. Please allow access to the USB device."
            )
        }
    }

    private fun onPermissionGranted(device: UsbDevice) {
        // Attempt to open the device connection
        val connection = usbManager.openDevice(device)
        if (connection == null) {
            _connectionState.value = UsbConnectionState.Error(
                "Could not open USB device. Is it properly plugged in?"
            )
            return
        }

        // Scan for model files on the USB volume
        val modelFiles = findModelFilesOnUsb()
        val modelPath = modelFiles.firstOrNull() ?: "/storage/usb/model.gguf"

        _connectionState.value = UsbConnectionState.Connected(
            deviceName = device.deviceName,
            modelPath  = modelPath
        )

        // Close the raw connection; we'll use file-system access for the model
        connection.close()
    }

    private fun handleDeviceAttached(intent: Intent) {
        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        device?.let { requestPermission(it) }
    }

    private fun handleDeviceDetached(intent: Intent) {
        _connectionState.value = UsbConnectionState.Disconnected
    }
}
