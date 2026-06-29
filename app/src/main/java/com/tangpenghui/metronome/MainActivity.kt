package com.tangpenghui.metronome

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tangpenghui.metronome.data.ExerciseRepository
import com.tangpenghui.metronome.data.MetronomeDatabase
import com.tangpenghui.metronome.service.MetronomeBinder
import com.tangpenghui.metronome.service.MetronomeService
import com.tangpenghui.metronome.ui.calendar.CalendarScreen
import com.tangpenghui.metronome.ui.calendar.CalendarViewModel
import com.tangpenghui.metronome.audio.BeatType
import com.tangpenghui.metronome.ui.home.HomeScreen
import com.tangpenghui.metronome.ui.home.HomeViewModel
import com.tangpenghui.metronome.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private var service: MetronomeService? = null
    private var binder by mutableStateOf<MetronomeBinder?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, ib: IBinder?) {
            Log.d(TAG, "Service connected")
            val b = ib as? MetronomeBinder ?: return
            binder = b
            service = b.service()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            binder = null; service = null
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkBatteryOptimization() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MetronomeService.start(this)
        bindService(Intent(this, MetronomeService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        checkBatteryOptimization()

        setContent {
            MetronomeTheme {
                AppRoot(binder)
            }
        }
    }

    private fun checkBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                batteryOptimizationLauncher.launch(intent)
            } catch (_: Throwable) {
                Toast.makeText(this,
                    "建议在系统设置中关闭本应用的电池优化，以保障锁屏后节拍器正常运行",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        try { unbindService(serviceConnection) } catch (_: Throwable) {}
        super.onDestroy()
    }

    companion object { private const val TAG = "MainActivity" }
}

@Composable
private fun AppRoot(binder: MetronomeBinder?) {
    val navController = rememberNavController()

    if (binder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController, startDestination = "home") {
        composable("home") {
            val triggerProvider: () -> BeatType = {
                val raw = binder.controller().audioEngine.visualTrigger
                when (raw) {
                    1 -> BeatType.HEAVY
                    2 -> BeatType.LIGHT
                    else -> BeatType.NONE
                }
            }
            HomeScreen(
                viewModel = HomeViewModel(binder),
                onOpenCalendar = { navController.navigate("calendar") },
                triggerProvider = triggerProvider
            )
        }
        composable("calendar") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val db = remember { MetronomeDatabase.get(context) }
            val repo = remember { ExerciseRepository(db.sessionDao()) }
            val calendarViewModel = remember { CalendarViewModel(repo) }
            CalendarScreen(
                viewModel = calendarViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
