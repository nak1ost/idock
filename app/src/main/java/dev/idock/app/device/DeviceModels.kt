package dev.idock.app.device

enum class DeviceMode(val label: String) {
    Normal("Обычный"),
    Recovery("Восстановление"),
    Dfu("DFU"),
}

data class BatteryInfo(
    val health: Int,
    val cycles: Int,
    val level: Int,
    val temperature: Int,
)

data class StoragePart(val label: String, val gb: Float)

data class StorageInfo(val totalGb: Float, val parts: List<StoragePart>) {
    val usedGb: Float get() = parts.sumOf { it.gb.toDouble() }.toFloat()
}

/**
 * What the dashboard shows about a device. Fields that the current connection level cannot
 * read stay null: nothing is ever filled in with a guess for a real device.
 */
data class DeviceInfo(
    val demo: Boolean,
    val title: String,
    val subtitle: String,
    val capacity: String?,
    /** The identifier printed under the barcode: serial number or UDID. */
    val code: String,
    val mode: DeviceMode,
    /** Rows of the device label; tapping a row copies its value. */
    val rows: List<Pair<String, String>>,
    val facts: List<Pair<String, String>>,
    val battery: BatteryInfo?,
    val storage: StorageInfo?,
)

sealed interface LinkState {
    data object Disconnected : LinkState
    data class NeedsPermission(val name: String) : LinkState
    data class Connected(val info: DeviceInfo) : LinkState
}

/** A made-up device for looking around the app without an iPhone. Every value here is fictional. */
object DemoDevice {
    val info = DeviceInfo(
        demo = true,
        title = "iPhone 14 Pro",
        subtitle = "Тёмно-фиолетовый · iOS 17.5.1",
        capacity = "128 ГБ",
        code = "DEMO7X2K9QF4",
        mode = DeviceMode.Normal,
        rows = listOf(
            "Серийный номер" to "DEMO7X2K9QF4",
            "UDID" to "00008120-001A2B3C4D5E6F70",
            "Сборка iOS" to "21F90",
            "Режим" to DeviceMode.Normal.label,
        ),
        facts = listOf(
            "Идентификатор модели" to "iPhone15,2",
            "Номер модели" to "MQ0G3",
            "Адрес Wi‑Fi" to "00:11:22:33:44:55",
            "Адрес Bluetooth" to "00:11:22:33:44:56",
            "Прошивка модема" to "2.60.02",
        ),
        battery = BatteryInfo(health = 87, cycles = 312, level = 64, temperature = 31),
        storage = StorageInfo(
            totalGb = 128f,
            parts = listOf(
                StoragePart("Приложения", 38.2f),
                StoragePart("Фото и видео", 27.6f),
                StoragePart("Медиа", 6.4f),
                StoragePart("Система", 10.2f),
            ),
        ),
    )
}
