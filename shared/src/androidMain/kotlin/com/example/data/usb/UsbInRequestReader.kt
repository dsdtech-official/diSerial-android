package com.example.data.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbRequest
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Keeps one USB IN request pending across the serial reader's framing deadlines. */
internal class UsbInRequestReader(
    private val connection: UsbDeviceConnection,
    endpoint: UsbEndpoint,
) {
    private val request = UsbRequest().also {
        if (!it.initialize(connection, endpoint)) {
            it.close()
            throw IOException("无法初始化 USB IN 请求")
        }
    }
    private val buffer = ByteBuffer.allocateDirect(endpoint.maxPacketSize).apply { limit(0) }
    private val executor = Executors.newSingleThreadExecutor { worker ->
        Thread(worker, "diSerial USB IN").apply { isDaemon = true }
    }
    private val lock = Any()
    private var pending: Future<UsbRequest?>? = null
    private var closed = false

    fun read(destination: ByteArray, timeoutMs: Int): Int {
        if (destination.isEmpty()) return 0
        val completion = synchronized(lock) {
            checkOpen()
            pending ?: run {
                if (buffer.hasRemaining()) return drain(destination)
                submit()
            }
        }
        val completed = try {
            if (timeoutMs == 0) completion.get() else completion.get(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            // The USB request stays queued. The caller can flush a frame and wait again.
            return 0
        } catch (failure: ExecutionException) {
            throw IOException("USB IN 读取失败", failure.cause)
        }
        return synchronized(lock) {
            checkOpen()
            if (completed !== request) throw IOException("USB IN 请求中断")
            pending = null
            buffer.flip()
            drain(destination)
        }
    }

    fun close(closeConnection: () -> Unit) {
        val cleanup = synchronized(lock) {
            if (closed) return
            closed = true
            if (pending != null) runCatching { request.cancel() }
            // Reap the cancelled request before freeing its connection and native buffer.
            executor.submit {
                try {
                    request.close()
                } finally {
                    closeConnection()
                }
            }.also { executor.shutdown() }
        }
        try {
            cleanup.get(250, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            // Cleanup remains queued if disconnection takes longer than the UI can wait.
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun submit(): Future<UsbRequest?> {
        buffer.clear()
        // Keep the API 12 overload for Android 7.0 and 7.1.
        @Suppress("DEPRECATION")
        if (!request.queue(buffer, buffer.capacity())) throw IOException("无法提交 USB IN 请求")
        return executor.submit<UsbRequest?> { connection.requestWait() }.also { pending = it }
    }

    private fun drain(destination: ByteArray): Int {
        val count = minOf(destination.size, buffer.remaining())
        buffer.get(destination, 0, count)
        return count
    }

    private fun checkOpen() {
        if (closed) throw IOException("串口未打开")
    }
}
