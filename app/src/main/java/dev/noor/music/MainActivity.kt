package dev.noor.music

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.noor.music.common.*
import dev.noor.music.presentation.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MusicViewModel = hiltViewModel()
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            val systemLanguage = LocalConfiguration.current.locales[0].language
            val fa =
                prefs.language == "fa" || (prefs.language == "system" && systemLanguage == "fa")
            CompositionLocalProvider(
                LocalPersian provides fa,
                LocalLayoutDirection provides if (fa) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                NoorTheme(prefs.light) {
                    val permission =
                        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                        else Manifest.permission.READ_EXTERNAL_STORAGE
                    var granted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(this, permission) ==
                                PackageManager.PERMISSION_GRANTED
                        )
                    }
                    SideEffect {
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = granted && prefs.light
                            isAppearanceLightNavigationBars = granted && prefs.light
                        }
                    }
                    val ask =
                        rememberLauncherForActivityResultCompat(
                            ActivityResultContracts.RequestPermission()
                        ) { result ->
                            granted = result
                            if (result) vm.permissionGranted()
                        }
                    val owner = LocalLifecycleOwner.current
                    DisposableEffect(owner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                val now =
                                    ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        permission,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (now && !granted) vm.permissionGranted()
                                granted = now
                            }
                        }
                        owner.lifecycle.addObserver(observer)
                        onDispose { owner.lifecycle.removeObserver(observer) }
                    }
                    LaunchedEffect(granted) { if (granted) vm.permissionGranted() }
                    val delete by vm.platformDelete.collectAsStateWithLifecycle()
                    val deleteLauncher =
                        rememberLauncherForActivityResultCompat(
                            ActivityResultContracts.StartIntentSenderForResult()
                        ) {
                            vm.deletionResult(it.resultCode == RESULT_OK)
                        }
                    LaunchedEffect(delete) {
                        delete?.let { uris ->
                            runCatching {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(
                                            MediaStore.createDeleteRequest(
                                                    contentResolver,
                                                    uris.map(Uri::parse),
                                                )
                                                .intentSender
                                        )
                                        .build()
                                )
                            }
                                .onSuccess { vm.platformDelete.value = null }
                                .onFailure {
                                    vm.message.value = it.message
                                    vm.deletionResult(false)
                                }
                        }
                    }
                    val write by vm.platformWrite.collectAsStateWithLifecycle()
                    val writeLauncher =
                        rememberLauncherForActivityResultCompat(
                            ActivityResultContracts.StartIntentSenderForResult()
                        ) {
                            vm.writeResult(it.resultCode == RESULT_OK)
                        }
                    LaunchedEffect(write) {
                        write?.let { uri ->
                            runCatching {
                                writeLauncher.launch(
                                    IntentSenderRequest.Builder(
                                            MediaStore.createWriteRequest(
                                                    contentResolver,
                                                    listOf(Uri.parse(uri)),
                                                )
                                                .intentSender
                                        )
                                        .build()
                                )
                            }
                                .onSuccess { vm.platformWrite.value = null }
                                .onFailure {
                                    vm.message.value = it.message
                                    vm.writeResult(false)
                                }
                        }
                    }
                    if (granted) MusicApp(vm)
                    else
                        NoorTheme {
                            WelcomeScreen(
                                onAllow = { ask.launch(permission) },
                                onSettings = {
                                    startActivity(
                                        android.content.Intent(
                                            android.provider.Settings
                                                .ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:$packageName"),
                                        )
                                    )
                                },
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun <I, O> rememberLauncherForActivityResultCompat(
    contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
) = androidx.activity.compose.rememberLauncherForActivityResult(contract, onResult)
