package com.offlineai.data.model

/**
 * Represents the current USB AI connection status.
 *
 * Used as a StateFlow value in the ViewModel so that the UI
 * can react to connection changes in real time.
 */
sealed class UsbConnectionState {
    /** No USB device has been detected or connected yet. */
    object Disconnected : UsbConnectionState()

    /** A USB device was found and we are attempting to open it. */
    object Connecting : UsbConnectionState()

    /**
     * Successfully connected to the USB device.
     *
     * @param deviceName   Android's internal USB device path (e.g. "/dev/bus/usb/001/002").
     * @param modelPath    Path to the AI model file on the USB storage.
     */
    data class Connected(
        val deviceName: String,
        val modelPath: String
    ) : UsbConnectionState()

    /**
     * An error occurred during connection or permission was denied.
     *
     * @param message   Human-readable error description.
     */
    data class Error(val message: String) : UsbConnectionState()
}
