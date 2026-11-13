package dev.idock.app.device

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Watches the USB bus for an iPhone, iPad or iPod touch plugged into this phone (USB host / OTG).
 *
 * This is the first connection level: it reads only what the USB descriptors say, namely the
 * product name, the manufacturer, the serial string (which on these devices is the UDID) and,
 * from the product id, whether the device is in normal, recovery or DFU mode. Nothing is written
 * to the device.
 */
class UsbDeviceMonitor(private val appContext: Context) {

    private val usb: UsbManager? = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _state = MutableStateFlow<LinkState>(LinkState.Disconnected)
    val state: StateFlow<LinkState> = _state

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    fun start() {
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(ACTION_PERMISSION)
            }
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
        refresh()
    }

    fun stop() {
        if (registered) {
            runCatching { appContext.unregisterReceiver(receiver) }
            registered = false
        }
    }

    fun refresh() {
        val device = findDevice()
        val manager = usb
        _state.value = when {
            device == null || manager == null -> LinkState.Disconnected
            !manager.hasPermission(device) -> LinkState.NeedsPermission(displayName(device))
            else -> LinkState.Connected(describe(device))
        }
    }

    /** Shows the system dialog that asks the user to let this app talk to the connected device. */
    fun requestPermission() {
        val manager = usb ?: return
        val device = findDevice() ?: return
        if (manager.hasPermission(device)) {
            refresh()
            return
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val intent = Intent(ACTION_PERMISSION).setPackage(appContext.packageName)
        manager.requestPermission(device, PendingIntent.getBroadcast(appContext, 0, intent, flags))
    }

    private fun findDevice(): UsbDevice? {
        val manager = usb ?: return null
        return runCatching {
            manager.deviceList.values.firstOrNull { it.vendorId == APPLE_VENDOR_ID && modeOf(it.productId) != null }
        }.getOrNull()
    }

    private fun displayName(device: UsbDevice): String =
        runCatching { device.productName }.getOrNull()?.takeIf { it.isNotBlank() } ?: kindOf(device.productId)

    private fun describe(device: UsbDevice): DeviceInfo {
        val mode = modeOf(device.productId) ?: DeviceMode.Normal
        val name = displayName(device)
        val maker = runCatching { device.manufacturerName }.getOrNull()?.takeIf { it.isNotBlank() }
        // Reading the serial string needs the USB permission on Android 10+, which we hold here.
        val udid = runCatching { device.serialNumber }.getOrNull()?.takeIf { it.isNotBlank() }?.let(::formatUdid)
        val usbId = String.format(Locale.ROOT, "%04X:%04X", device.vendorId, device.productId)

        val rows = buildList {
            if (udid != null) add((if (mode == DeviceMode.Normal) "UDID" else "Серийная строка") to udid)
            add("Режим" to mode.label)
            add("USB ID" to usbId)
            if (maker != null) add("Производитель" to maker)
        }
        return DeviceInfo(
            demo = false,
            title = name,
            subtitle = when (mode) {
                DeviceMode.Normal -> "Подключён по USB"
                DeviceMode.Recovery -> "Режим восстановления"
                DeviceMode.Dfu -> "Режим DFU"
            },
            capacity = null,
            code = udid ?: usbId,
            mode = mode,
            rows = rows,
            facts = emptyList(),
            battery = null,
            storage = null,
        )
    }

    companion object {
        const val APPLE_VENDOR_ID = 0x05AC
        private const val ACTION_PERMISSION = "dev.idock.app.USB_PERMISSION"

        /** Classifies an Apple product id; null means "not a phone or tablet" (keyboard, headphones…). */
        fun modeOf(productId: Int): DeviceMode? = when (productId) {
            0x1227, 0x1222 -> DeviceMode.Dfu
            in 0x1280..0x1283 -> DeviceMode.Recovery
            in 0x1290..0x12AF -> DeviceMode.Normal
            else -> null
        }

        fun kindOf(productId: Int): String = when (productId) {
            0x12AB -> "iPad"
            0x12AA -> "iPod touch"
            else -> "iPhone"
        }

        /** Newer devices report a 24-character id; the usual notation has a dash after the 8th character. */
        fun formatUdid(raw: String): String {
            val clean = raw.trim()
            return if (clean.length == 24 && '-' !in clean) clean.substring(0, 8) + "-" + clean.substring(8) else clean
        }
    }
}
