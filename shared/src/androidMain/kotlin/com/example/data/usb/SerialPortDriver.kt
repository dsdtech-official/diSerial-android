package com.example.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.example.ui.model.SerialPortConfig
import java.io.IOException

enum class UsbDriverType(val displayName: String) {
    CH34X("CH34x"),
    CP210X("CP210x"),
    FTDI("FTDI"),
    PL2303("PL2303"),
    CDC_ACM("CDC-ACM"),
}

data class DetectedUsbChip(
    val driverType: UsbDriverType,
    val chipName: String,
)

interface SerialPortDriver {
    val driverType: UsbDriverType

    @Throws(IOException::class)
    fun open(connection: UsbDeviceConnection)

    @Throws(IOException::class)
    fun setParameters(config: SerialPortConfig)

    @Throws(IOException::class)
    fun setDtr(enabled: Boolean)

    @Throws(IOException::class)
    fun setRts(enabled: Boolean)

    @Throws(IOException::class)
    fun read(dest: ByteArray, timeoutMs: Int): Int

    @Throws(IOException::class)
    fun write(src: ByteArray, timeoutMs: Int): Int

    fun close()
}

object UsbChipCatalog {
    private val wchModel = Regex("\\bCH(?:340[CGKN]?|341[AB]?|343[GP]?|9102[FX]?)\\b", RegexOption.IGNORE_CASE)
    private val cp210xModel = Regex("\\bCP210(?:2N?|[34589])\\b", RegexOption.IGNORE_CASE)
    private val ftdiModel = Regex("\\bFT(?:232(?:R[LQ]?|H|B)?|2232[DH]?|4232H?|23[01]X)\\b", RegexOption.IGNORE_CASE)
    private val pl2303Model = Regex("\\bPL2303\\b", RegexOption.IGNORE_CASE)

    fun shortModelFromProductName(driverType: UsbDriverType, productName: String?): String? {
        if (productName.isNullOrBlank()) return null
        val pattern = when (driverType) {
            UsbDriverType.CH34X, UsbDriverType.CDC_ACM -> wchModel
            UsbDriverType.CP210X -> cp210xModel
            UsbDriverType.FTDI -> ftdiModel
            UsbDriverType.PL2303 -> pl2303Model
        }
        return pattern.findAll(productName)
            .map { it.value.uppercase() }
            .distinct()
            .singleOrNull()
    }

    fun probe(device: UsbDevice): DetectedUsbChip? {
        val vid = device.vendorId
        val pid = device.productId

        // 1. WCH CH340 / CH341 / CH343 / CH9102
        if (vid == 0x1A86) {
            return when (pid) {
                0x7523 -> DetectedUsbChip(UsbDriverType.CH34X, "WCH CH340/CH341 USB-UART")
                0x5523 -> DetectedUsbChip(UsbDriverType.CH34X, "WCH CH341 USB-UART")
                0x7522 -> DetectedUsbChip(UsbDriverType.CH34X, "WCH CH340K USB-UART")
                0x55D4 -> DetectedUsbChip(UsbDriverType.CDC_ACM, "WCH CH9102 USB-UART")
                0x55D3 -> DetectedUsbChip(UsbDriverType.CDC_ACM, "WCH CH343 USB-UART")
                else -> null
            }
        }

        // 2. Silicon Labs CP210x
        if (vid == 0x10C4 && pid in listOf(0xEA60, 0xEA70, 0xEA71, 0xEA80)) {
            val label = when (pid) {
                0xEA60 -> "Silicon Labs CP2102/CP2109"
                0xEA70 -> "Silicon Labs CP2105 Dual"
                0xEA71 -> "Silicon Labs CP2108 Quad"
                else -> "Silicon Labs CP210x"
            }
            return DetectedUsbChip(UsbDriverType.CP210X, label)
        }

        // 3. FTDI
        if (vid == 0x0403 && pid in listOf(0x6001, 0x6010, 0x6011, 0x6014, 0x6015)) {
            val label = when (pid) {
                0x6001 -> "FTDI FT232R USB-UART"
                0x6010 -> "FTDI FT2232 Dual"
                0x6014 -> "FTDI FT232H"
                0x6015 -> "FTDI FT230X/FT231X"
                else -> "FTDI USB-UART"
            }
            return DetectedUsbChip(UsbDriverType.FTDI, label)
        }

        // 4. Prolific PL2303
        if (vid == 0x067B && pid in listOf(0x2303, 0x23A3, 0x23B3, 0x23C3, 0x23D3, 0x23E3, 0x23F3)) {
            return DetectedUsbChip(UsbDriverType.PL2303, "Prolific PL2303 USB-UART")
        }

        // 5. Standard USB CDC-ACM (STM32, ESP32-S3, Arduino, RP2040, etc.)
        val hasCdcInterface = (0 until device.interfaceCount).any { idx ->
            device.getInterface(idx).interfaceClass == UsbConstants.USB_CLASS_COMM
        }
        if (device.deviceClass == UsbConstants.USB_CLASS_COMM || hasCdcInterface) {
            val product = device.productName?.takeIf { it.isNotBlank() } ?: "USB CDC-ACM"
            return DetectedUsbChip(UsbDriverType.CDC_ACM, product)
        }

        return null
    }

    fun createDriver(type: UsbDriverType, device: UsbDevice): SerialPortDriver {
        return when (type) {
            UsbDriverType.CH34X -> Ch34xSerialDriver(device)
            UsbDriverType.CP210X -> Cp210xSerialDriver(device)
            UsbDriverType.FTDI -> FtdiSerialDriver(device)
            UsbDriverType.PL2303 -> Pl2303SerialDriver(device)
            UsbDriverType.CDC_ACM -> CdcAcmSerialDriver(device)
        }
    }
}
