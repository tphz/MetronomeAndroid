package com.tangpenghui.metronome

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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
import com.tangpenghui.metronome.ui.home.HomeScreen
import com.tangpenghui.metronome.ui.home.HomeViewModel
import com.tangpenghui.metronome.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private var service: MetronomeService? = null
    private var binder: MetronomeBinder? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, ib: IBinder?) {
            val b = ib as? MetronomeBinder ?: return
            binder = b
            service = b.service()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null; service = null
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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

        setContent {
            MetronomeTheme {
                AppRoot(binderProvider = { binder })
            }
        }
    }

    override fun onDestroy() {
        try { unbindService(serviceConnection) } catch (_: Throwable) {}
        super.onDestroy()
    }
}

@Composable
private fun AppRoot(binderProvider: () -> MetronomeBinder?) {
    val navController = rememberNavController()
    val binder = binderProvider()

    if (binder == null) {
        androidx.compose.material3.CircularProgressIndicator()
        return
    }

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = HomeViewModel(binder),
                onOpenCalendar = { navController.navigate("calendar") }
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
