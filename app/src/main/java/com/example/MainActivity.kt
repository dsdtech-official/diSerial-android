package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ExportFormat
import com.example.data.local.LogExporter
import com.example.platform.PlatformFileHandle
import com.example.platform.resolveSystemLanguageTag
import com.example.ui.DiSerialApp
import com.example.ui.i18n.resolveAppStrings
import com.example.ui.model.SavedLogSession
import com.example.ui.screens.LocalPrivacyPolicyUrl
import com.example.ui.state.AndroidSerialViewModelFactory
import com.example.ui.state.SerialUiViewModel
import com.example.ui.state.handleLaunchedUsbDevice
import com.example.ui.theme.SerialScopeTheme
import com.example.ui.theme.resolveIsDarkTheme

private const val ENABLE_FILE_SEND = false
private const val ANDROID_PRIVACY_POLICY_URL = "https://dsdtech-official.github.io/diSerial-android/privacy/"

class MainActivity : ComponentActivity() {

    private val serialViewModel: SerialUiViewModel by lazy {
        ViewModelProvider(
            this,
            AndroidSerialViewModelFactory(applicationContext),
        )[SerialUiViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        if (savedInstanceState == null) {
            extractAttachedUsbDevice(intent)?.let { usbDevice ->
                serialViewModel.handleLaunchedUsbDevice(usbDevice)
            }
        }
        setContent {
            CompositionLocalProvider(LocalPrivacyPolicyUrl provides ANDROID_PRIVACY_POLICY_URL) {
                AndroidSerialScopeShell(viewModel = serialViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractAttachedUsbDevice(intent)?.let { usbDevice ->
            serialViewModel.handleLaunchedUsbDevice(usbDevice)
        }
    }

    private fun extractAttachedUsbDevice(intent: Intent?): UsbDevice? {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}

@Composable
private fun AndroidSerialScopeShell(
    viewModel: SerialUiViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentStrings = remember(state.settings.appLanguage, context) {
        resolveAppStrings(state.settings.appLanguage, resolveSystemLanguageTag(context))
    }

    var pendingExportSession by remember { mutableStateOf<SavedLogSession?>(null) }
    var pendingExportFormat by remember { mutableStateOf(ExportFormat.TXT) }

    val txtExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val session = pendingExportSession
        if (uri != null && session != null) {
            viewModel.exportSession(
                target = PlatformFileHandle(uri.toString()),
                session = session,
                format = pendingExportFormat,
            )
        }
        pendingExportSession = null
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val session = pendingExportSession
        if (uri != null && session != null) {
            viewModel.exportSession(
                target = PlatformFileHandle(uri.toString()),
                session = session,
                format = pendingExportFormat,
            )
        }
        pendingExportSession = null
    }

    val onPickFileToSend = if (ENABLE_FILE_SEND) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                viewModel.prepareFileSend(PlatformFileHandle(uri.toString()))
            }
        }
        val pickFile: () -> Unit = { launcher.launch(arrayOf("*/*")) }
        pickFile
    } else null

    val macroImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importMacros(PlatformFileHandle(uri.toString()))
        }
    }

    val macroExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportMacros(PlatformFileHandle(uri.toString()))
        }
    }

    val isDarkTheme = resolveIsDarkTheme(state.settings.themeMode)
    DisposableEffect(isDarkTheme, context) {
        val componentActivity = context as? ComponentActivity
        val barStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        componentActivity?.enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle,
        )
        onDispose {}
    }

    DisposableEffect(state.settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (state.settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    SerialScopeTheme(themeMode = state.settings.themeMode) {
        DiSerialApp(
            viewModel = viewModel,
            appVersion = BuildConfig.VERSION_NAME,
        onPickFileToSend = onPickFileToSend,
        onImportMacros = {
            macroImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onExportMacros = { suggestedName ->
            macroExportLauncher.launch(suggestedName)
        },
        onExportLogDocument = { session, format, suggestedName ->
            pendingExportSession = session
            pendingExportFormat = format
            when (format) {
                ExportFormat.TXT -> txtExportLauncher.launch(suggestedName)
                ExportFormat.CSV -> csvExportLauncher.launch(suggestedName)
            }
        },
        onShareLog = { session, format ->
            val s = currentStrings
            viewModel.shareSession(session, format) { uriStr ->
                val shareUri = Uri.parse(uriStr)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mimeType
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, LogExporter.buildEmailSubject(session, s))
                    clipData = ClipData.newRawUri(
                        LogExporter.buildSuggestedFileName(session, format),
                        shareUri,
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, s.shareLogChooserTitle))
                } catch (_: ActivityNotFoundException) {
                    viewModel.postNotice(s.noShareAppNotice)
                }
            }
        },
        onEmailLog = { format, session ->
            val s = currentStrings
            viewModel.shareSession(session, format) { uriStr ->
                val shareUri = Uri.parse(uriStr)
                val subject = LogExporter.buildEmailSubject(session, s)
                val body = LogExporter.buildEmailBody(session, format, s)
                val fileName = LogExporter.buildSuggestedFileName(session, format)
                val mailSelector = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                }
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    selector = mailSelector
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    clipData = ClipData.newRawUri(fileName, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val hasEmailClient = emailIntent.resolveActivity(context.packageManager) != null
                val targetIntent = if (hasEmailClient) {
                    Intent.createChooser(emailIntent, s.emailClientChooserTitle)
                } else {
                    viewModel.postNotice(s.noEmailFallbackNotice)
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = format.mimeType
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        clipData = ClipData.newRawUri(fileName, shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    Intent.createChooser(
                        fallbackIntent,
                        s.emailFallbackChooserTitle(format.extension.uppercase()),
                    )
                }
                try {
                    context.startActivity(targetIntent)
                } catch (_: ActivityNotFoundException) {
                    viewModel.postNotice(s.noEmailOrShareAppNotice)
                }
            }
        },
        )
    }
}
