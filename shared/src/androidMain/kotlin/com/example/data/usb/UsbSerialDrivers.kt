package com.example.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.example.ui.model.ParityMode
import com.example.ui.model.SerialPortConfig
import java.io.IOException
import kotlin.math.roundToInt

abstract class BaseUsbSerialDriver(
    protected val device: UsbDevice,
) : SerialPortDriver {

    protected var connection: UsbDeviceConnection? = null
    protected var readEndpoint: UsbEndpoint? = null
    protected var writeEndpoint: UsbEndpoint? = null
    protected val claimedInterfaces = mutableListOf<UsbInterface>()
    protected var dtrState: Boolean = false
    protected var rtsState: Boolean = false

    protected fun claimInterface(iface: UsbInterface) {
        val conn = connection ?: throw IOException("USB connection not open")
        if (!conn.claimInterface(iface, true)) {
            throw IOException("无法独占 USB 接口 #${iface.id}")
        }
        claimedInterfaces.add(iface)
    }

    protected fun findBulkEndpoints(iface: UsbInterface) {
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN && readEndpoint == null) {
                    readEndpoint = ep
                } else if (ep.direction == UsbConstants.USB_DIR_OUT && writeEndpoint == null) {
                    writeEndpoint = ep
                }
            }
        }
    }

    override fun read(dest: ByteArray, timeoutMs: Int): Int {
        val conn = connection ?: throw IOException("串口未打开")
        val ep = readEndpoint ?: throw IOException("缺少 USB IN 端点")
        val count = conn.bulkTransfer(ep, dest, dest.size, timeoutMs)
        return if (count < 0) 0 else count
    }

    override fun write(src: ByteArray, timeoutMs: Int): Int {
        val conn = connection ?: throw IOException("串口未打开")
        val ep = writeEndpoint ?: throw IOException("缺少 USB OUT 端点")
        var offset = 0
        while (offset < src.size) {
            val chunkLen = (src.size - offset).coerceAtMost(ep.maxPacketSize.coerceAtLeast(64))
            val chunk = if (offset == 0 && chunkLen == src.size) {
                src
            } else {
                src.copyOfRange(offset, offset + chunkLen)
            }
            val written = conn.bulkTransfer(ep, chunk, chunk.size, timeoutMs)
            if (written <= 0) {
                throw IOException("USB 写入失败 ($written)")
            }
            offset += written
        }
        return offset
    }

    override fun close() {
        val conn = connection
        if (conn != null) {
            for (iface in claimedInterfaces) {
                runCatching { conn.releaseInterface(iface) }
            }
            runCatching { conn.close() }
        }
        claimedInterfaces.clear()
        readEndpoint = null
        writeEndpoint = null
        connection = null
    }
}

class CdcAcmSerialDriver(device: UsbDevice) : BaseUsbSerialDriver(device) {
    override val driverType = UsbDriverType.CDC_ACM
    private var controlInterfaceIndex = 0

    override fun open(connection: UsbDeviceConnection) {
        this.connection = connection
        var controlIface: UsbInterface? = null
        var dataIface: UsbInterface? = null

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            when (iface.interfaceClass) {
                UsbConstants.USB_CLASS_COMM -> if (controlIface == null) controlIface = iface
                UsbConstants.USB_CLASS_CDC_DATA -> if (dataIface == null) dataIface = iface
            }
        }

        if (controlIface != null) {
            controlInterfaceIndex = controlIface.id
            claimInterface(controlIface)
            findBulkEndpoints(controlIface)
        }
        if (dataIface != null) {
            claimInterface(dataIface)
            findBulkEndpoints(dataIface)
        }
        if (readEndpoint == null || writeEndpoint == null) {
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (!claimedInterfaces.contains(iface)) {
                    claimInterface(iface)
                }
                findBulkEndpoints(iface)
            }
        }
        if (readEndpoint == null || writeEndpoint == null) {
            close()
            throw IOException("未找到 CDC-ACM 数据读写端点")
        }
    }

    override fun setParameters(config: SerialPortConfig) {
        val conn = connection ?: throw IOException("串口未打开")
        val stopBitsByte = when (config.stopBits) {
            "1" -> 0
            "1.5" -> 1
            "2" -> 2
            else -> 0
        }.toByte()
        val parityByte = when (config.parity) {
            ParityMode.NONE -> 0
            ParityMode.ODD -> 1
            ParityMode.EVEN -> 2
            ParityMode.MARK -> 3
            ParityMode.SPACE -> 4
        }.toByte()

        val baud = config.baudRate
        val lineCoding = byteArrayOf(
            (baud and 0xFF).toByte(),
            ((baud ushr 8) and 0xFF).toByte(),
            ((baud ushr 16) and 0xFF).toByte(),
            ((baud ushr 24) and 0xFF).toByte(),
            stopBitsByte,
            parityByte,
            config.dataBits.toByte(),
        )

        val res = conn.controlTransfer(
            0x21,
            0x20,
            0,
            controlInterfaceIndex,
            lineCoding,
            lineCoding.size,
            2000,
        )
        if (res < 0) throw IOException("CDC SET_LINE_CODING 失败")

        dtrState = config.dtrEnabled
        rtsState = config.rtsEnabled
        updateModemLines()
    }

    override fun setDtr(enabled: Boolean) {
        dtrState = enabled
        updateModemLines()
    }

    override fun setRts(enabled: Boolean) {
        rtsState = enabled
        updateModemLines()
    }

    private fun updateModemLines() {
        val conn = connection ?: return
        val value = (if (dtrState) 0x01 else 0x00) or (if (rtsState) 0x02 else 0x00)
        conn.controlTransfer(0x21, 0x22, value, controlInterfaceIndex, null, 0, 2000)
    }
}

class Ch34xSerialDriver(device: UsbDevice) : BaseUsbSerialDriver(device) {
    override val driverType = UsbDriverType.CH34X

    override fun open(connection: UsbDeviceConnection) {
        this.connection = connection
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            claimInterface(iface)
            findBulkEndpoints(iface)
        }
        if (readEndpoint == null || writeEndpoint == null) {
            close()
            throw IOException("未找到 CH34x 串口端点")
        }

        // CH34x initialization handshake
        val buf = ByteArray(8)
        ctrlIn(0x5F, 0, 0, buf)
        ctrlOut(0xA1, 0, 0)
        ctrlOut(0x9A, 0x1312, 0xD982)
        ctrlOut(0x9A, 0x0F2C, 0x0007)
        ctrlIn(0x95, 0x2518, 0, buf)
        ctrlOut(0x9A, 0x2727, 0)
    }

    override fun setParameters(config: SerialPortConfig) {
        val (index, factor) = computeCh34xBaudDivisor(config.baudRate)
        ctrlOut(0x9A, 0x1312, index)
        ctrlOut(0x9A, 0x0F2C, factor)

        var lcr = 0xC0 // Enable RX & TX
        lcr = lcr or when (config.dataBits) {
            5 -> 0x00
            6 -> 0x01
            7 -> 0x02
            else -> 0x03
        }
        if (config.stopBits == "2") lcr = lcr or 0x04
        when (config.parity) {
            ParityMode.NONE -> Unit
            ParityMode.ODD -> lcr = lcr or 0x08
            ParityMode.EVEN -> lcr = lcr or 0x18
            ParityMode.MARK -> lcr = lcr or 0x28
            ParityMode.SPACE -> lcr = lcr or 0x38
        }
        ctrlOut(0x9A, 0x2518, lcr)

        dtrState = config.dtrEnabled
        rtsState = config.rtsEnabled
        updateModemLines()
    }

    override fun setDtr(enabled: Boolean) {
        dtrState = enabled
        updateModemLines()
    }

    override fun setRts(enabled: Boolean) {
        rtsState = enabled
        updateModemLines()
    }

    private fun updateModemLines() {
        val v = ((if (dtrState) 0 else 1 shl 5) or (if (rtsState) 0 else 1 shl 6))
        ctrlOut(0xA4, v.inv() and 0xFF, 0)
    }

    private fun ctrlOut(request: Int, value: Int, index: Int) {
        val conn = connection ?: throw IOException("串口未打开")
        val res = conn.controlTransfer(0x40, request, value, index, null, 0, 2000)
        if (res < 0) throw IOException("CH34x 控制传输失败: req=0x${request.toString(16)}")
    }

    private fun ctrlIn(request: Int, value: Int, index: Int, buffer: ByteArray) {
        val conn = connection ?: throw IOException("串口未打开")
        conn.controlTransfer(0xC0, request, value, index, buffer, buffer.size, 2000)
    }

    private fun computeCh34xBaudDivisor(baudRate: Int): Pair<Int, Int> {
        val baud = baudRate.coerceIn(300, 2_000_000)
        var factor = 1532620800 / baud
        var divisor = 3
        while (factor > 65520 && divisor > 0) {
            factor = factor ushr 3
            divisor--
        }
        val low = 0x100 - factor
        val a = (low and 0xFF) shl 8
        val b = divisor or 0x80
        return (a or b) to 0
    }
}

class Cp210xSerialDriver(device: UsbDevice) : BaseUsbSerialDriver(device) {
    override val driverType = UsbDriverType.CP210X
    private var interfaceIndex = 0
    private var inputReader: UsbInRequestReader? = null

    override fun open(connection: UsbDeviceConnection) {
        this.connection = connection
        inputReader = null
        val iface = device.getInterface(0)
        interfaceIndex = iface.id
        claimInterface(iface)
        findBulkEndpoints(iface)
        val endpoint = readEndpoint
        if (endpoint == null || writeEndpoint == null) {
            close()
            throw IOException("未找到 CP210x 串口端点")
        }
        // Enable UART interface
        ctrlOut(0x00, 0x0001, null)
        inputReader = UsbInRequestReader(connection, endpoint)
    }

    override fun read(dest: ByteArray, timeoutMs: Int): Int {
        val reader = inputReader ?: throw IOException("串口未打开")
        return reader.read(dest, timeoutMs)
    }

    override fun close() {
        val reader = inputReader
        if (reader == null) super.close() else reader.close { super.close() }
    }

    override fun setParameters(config: SerialPortConfig) {
        val baud = config.baudRate
        val baudBytes = byteArrayOf(
            (baud and 0xFF).toByte(),
            ((baud ushr 8) and 0xFF).toByte(),
            ((baud ushr 16) and 0xFF).toByte(),
            ((baud ushr 24) and 0xFF).toByte(),
        )
        ctrlOut(0x1E, 0, baudBytes)

        var lineCtl = (config.dataBits shl 8)
        lineCtl = lineCtl or when (config.parity) {
            ParityMode.NONE -> 0x0000
            ParityMode.ODD -> 0x0010
            ParityMode.EVEN -> 0x0020
            ParityMode.MARK -> 0x0030
            ParityMode.SPACE -> 0x0040
        }
        lineCtl = lineCtl or when (config.stopBits) {
            "1.5" -> 0x0001
            "2" -> 0x0002
            else -> 0x0000
        }
        ctrlOut(0x03, lineCtl, null)

        dtrState = config.dtrEnabled
        rtsState = config.rtsEnabled
        updateModemLines()
    }

    override fun setDtr(enabled: Boolean) {
        dtrState = enabled
        updateModemLines()
    }

    override fun setRts(enabled: Boolean) {
        rtsState = enabled
        updateModemLines()
    }

    private fun updateModemLines() {
        val mhs = 0x0300 or (if (dtrState) 0x0001 else 0x0000) or (if (rtsState) 0x0002 else 0x0000)
        ctrlOut(0x07, mhs, null)
    }

    private fun ctrlOut(request: Int, value: Int, data: ByteArray?) {
        val conn = connection ?: throw IOException("串口未打开")
        val res = conn.controlTransfer(0x41, request, value, interfaceIndex, data, data?.size ?: 0, 2000)
        if (res < 0) throw IOException("CP210x 控制传输失败: req=0x${request.toString(16)}")
    }
}

class FtdiSerialDriver(device: UsbDevice) : BaseUsbSerialDriver(device) {
    override val driverType = UsbDriverType.FTDI
    private var portIndex = 1
    private var rawPacketBuffer = ByteArray(0)
    private var pendingOffset = 0
    private var pendingEnd = 0

    override fun open(connection: UsbDeviceConnection) {
        pendingOffset = 0
        pendingEnd = 0
        this.connection = connection
        val iface = device.getInterface(0)
        portIndex = iface.id + 1
        claimInterface(iface)
        findBulkEndpoints(iface)
        if (readEndpoint == null || writeEndpoint == null) {
            close()
            throw IOException("未找到 FTDI 串口端点")
        }
        // Reset FTDI SIO
        ctrlOut(0x00, 0x0000)
    }

    override fun setParameters(config: SerialPortConfig) {
        val (value, index) = computeFtdiBaud(config.baudRate)
        val conn = connection ?: throw IOException("串口未打开")
        val res = conn.controlTransfer(0x40, 0x03, value, index, null, 0, 2000)
        if (res < 0) throw IOException("FTDI 波特率设置失败")

        var dataVal = config.dataBits
        dataVal = dataVal or when (config.parity) {
            ParityMode.NONE -> 0x0000
            ParityMode.ODD -> 0x0100
            ParityMode.EVEN -> 0x0200
            ParityMode.MARK -> 0x0300
            ParityMode.SPACE -> 0x0400
        }
        if (config.stopBits == "2") dataVal = dataVal or 0x1000
        ctrlOut(0x04, dataVal)

        dtrState = config.dtrEnabled
        rtsState = config.rtsEnabled
        updateModemLines()
    }

    override fun setDtr(enabled: Boolean) {
        dtrState = enabled
        updateModemLines()
    }

    override fun setRts(enabled: Boolean) {
        rtsState = enabled
        updateModemLines()
    }

    override fun read(dest: ByteArray, timeoutMs: Int): Int {
        val conn = connection ?: throw IOException("串口未打开")
        val ep = readEndpoint ?: throw IOException("缺少 USB IN 端点")
        if (dest.isEmpty()) return 0

        if (pendingOffset == pendingEnd) {
            val packetSize = ep.maxPacketSize
            if (rawPacketBuffer.size != packetSize) {
                rawPacketBuffer = ByteArray(packetSize)
            }
            // One packet can complete without waiting for later packets or a timeout.
            val received = conn.bulkTransfer(ep, rawPacketBuffer, packetSize, timeoutMs)
            if (received <= FTDI_STATUS_BYTES) return 0
            pendingOffset = FTDI_STATUS_BYTES
            pendingEnd = received
        }

        // Drain unread payload before reading the next packet.
        val count = minOf(dest.size, pendingEnd - pendingOffset)
        rawPacketBuffer.copyInto(dest, endIndex = pendingOffset + count, startIndex = pendingOffset)
        pendingOffset += count
        return count
    }

    private fun updateModemLines() {
        val dtrVal = if (dtrState) 0x0101 else 0x0100
        val rtsVal = if (rtsState) 0x0202 else 0x0200
        ctrlOut(0x01, dtrVal)
        ctrlOut(0x01, rtsVal)
    }

    private fun ctrlOut(request: Int, value: Int) {
        val conn = connection ?: throw IOException("串口未打开")
        val res = conn.controlTransfer(0x40, request, value, portIndex, null, 0, 2000)
        if (res < 0) throw IOException("FTDI 控制传输失败: req=0x${request.toString(16)}")
    }

    private fun computeFtdiBaud(baudRate: Int): Pair<Int, Int> {
        val divisorFloat = 3_000_000f / baudRate.coerceAtLeast(300)
        val bestDivisor = (divisorFloat * 8f).roundToInt()
        val fracCodes = intArrayOf(0, 3, 2, 4, 1, 5, 6, 7)
        val divisor = (bestDivisor ushr 3) or (fracCodes[bestDivisor and 0x07] shl 14)
        val value = divisor and 0xFFFF
        val index = ((divisor ushr 16) and 0xFF) or (portIndex shl 8)
        return value to index
    }

    private companion object {
        const val FTDI_STATUS_BYTES = 2
    }
}

class Pl2303SerialDriver(device: UsbDevice) : BaseUsbSerialDriver(device) {
    override val driverType = UsbDriverType.PL2303

    override fun open(connection: UsbDeviceConnection) {
        this.connection = connection
        val iface = device.getInterface(0)
        claimInterface(iface)
        findBulkEndpoints(iface)
        if (readEndpoint == null || writeEndpoint == null) {
            close()
            throw IOException("未找到 PL2303 串口端点")
        }

        val buf = ByteArray(1)
        vendorIn(0x8484, 0, buf)
        vendorOut(0x0404, 0)
        vendorIn(0x8484, 0, buf)
        vendorIn(0x8383, 0, buf)
        vendorIn(0x8484, 0, buf)
        vendorOut(0x0404, 1)
        vendorIn(0x8484, 0, buf)
        vendorIn(0x8383, 0, buf)
        vendorOut(0, 1)
        vendorOut(1, 0)
        vendorOut(2, 0x44)
    }

    override fun setParameters(config: SerialPortConfig) {
        val conn = connection ?: throw IOException("串口未打开")
        val baud = config.baudRate
        val stopBitsByte = when (config.stopBits) {
            "1.5" -> 1
            "2" -> 2
            else -> 0
        }.toByte()
        val parityByte = when (config.parity) {
            ParityMode.NONE -> 0
            ParityMode.ODD -> 1
            ParityMode.EVEN -> 2
            ParityMode.MARK -> 3
            ParityMode.SPACE -> 4
        }.toByte()

        val lineCoding = byteArrayOf(
            (baud and 0xFF).toByte(),
            ((baud ushr 8) and 0xFF).toByte(),
            ((baud ushr 16) and 0xFF).toByte(),
            ((baud ushr 24) and 0xFF).toByte(),
            stopBitsByte,
            parityByte,
            config.dataBits.toByte(),
        )
        val res = conn.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 2000)
        if (res < 0) throw IOException("PL2303 串口参数设置失败")

        dtrState = config.dtrEnabled
        rtsState = config.rtsEnabled
        updateModemLines()
    }

    override fun setDtr(enabled: Boolean) {
        dtrState = enabled
        updateModemLines()
    }

    override fun setRts(enabled: Boolean) {
        rtsState = enabled
        updateModemLines()
    }

    private fun updateModemLines() {
        val conn = connection ?: return
        val value = (if (dtrState) 0x01 else 0x00) or (if (rtsState) 0x02 else 0x00)
        conn.controlTransfer(0x21, 0x22, value, 0, null, 0, 2000)
    }

    private fun vendorIn(value: Int, index: Int, buf: ByteArray) {
        connection?.controlTransfer(0xC0, 0x01, value, index, buf, buf.size, 1000)
    }

    private fun vendorOut(value: Int, index: Int) {
        connection?.controlTransfer(0x40, 0x01, value, index, null, 0, 1000)
    }
}
