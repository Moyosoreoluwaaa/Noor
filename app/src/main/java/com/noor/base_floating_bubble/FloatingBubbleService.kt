package com.noor.base_floating_bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.noor.ui.theme.NoorTheme
import timber.log.Timber

// MyLifecycleOwner.kt - Custom lifecycle owner for ComposeView
class MyLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

}

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: ComposeView? = null
    private var expandedNoteView: ComposeView? = null
    private var isExpanded by mutableStateOf(false)
    private var noteText by mutableStateOf("")

    // Lifecycle owners for ComposeView
    private lateinit var bubbleLifecycleOwner: MyLifecycleOwner
    private lateinit var expandedLifecycleOwner: MyLifecycleOwner
    private lateinit var bubbleViewModelStore: ViewModelStore
    private lateinit var expandedViewModelStore: ViewModelStore

    companion object {
        const val ACTION_START_FLOATING = "start_floating"
        const val ACTION_STOP_FLOATING = "stop_floating"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "floating_bubble_channel"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        loadNote()

        // Initialize lifecycle owners and view model stores
        bubbleLifecycleOwner = MyLifecycleOwner()
        expandedLifecycleOwner = MyLifecycleOwner()
        bubbleViewModelStore = ViewModelStore()
        expandedViewModelStore = ViewModelStore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("FloatingBubbleService")
            .d("onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_FLOATING -> {
                Timber.tag("FloatingBubbleService").d("Starting floating bubble")
                showFloatingBubble()
            }

            ACTION_STOP_FLOATING -> {
                Timber.tag("FloatingBubbleService").d("Stopping floating bubble")
                stopFloatingBubble()
            }
        }
        return START_STICKY
    }

    private fun showFloatingBubble() {
        Timber.tag("FloatingBubbleService").d("showFloatingBubble called")
        if (floatingBubbleView != null) {
            Timber.tag("FloatingBubbleService").d("Floating bubble already exists")
            return
        }

        try {
            Timber.tag("FloatingBubbleService").d("Creating notification and starting foreground")
            startForeground(NOTIFICATION_ID, createNotification())

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100

            Timber.tag("FloatingBubbleService").d("Creating ComposeView")
            floatingBubbleView = ComposeView(this)

            // Set up lifecycle for ComposeView - this is the key part!
            bubbleLifecycleOwner.performRestore(null)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            // Set lifecycle owners using the correct API
            floatingBubbleView?.setViewTreeLifecycleOwner(bubbleLifecycleOwner)
            floatingBubbleView?.setViewTreeSavedStateRegistryOwner(bubbleLifecycleOwner)
            floatingBubbleView?.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore get() = bubbleViewModelStore
            })

            // Set content after lifecycle owner is set
            floatingBubbleView?.setContent {
                FloatingBubbleComposable(
                    onBubbleClick = {
                        Timber.tag("FloatingBubbleService").d("Bubble clicked")
                        toggleExpandedView()
                    })
            }

            Timber.tag("FloatingBubbleService").d("Adding view to window manager")
            windowManager?.addView(floatingBubbleView, params)
            Timber.tag("FloatingBubbleService").d("Floating bubble created successfully")

        } catch (e: Exception) {
            Timber.tag("FloatingBubbleService").e(e, "Error creating floating bubble")
        }
    }

    @Composable
    private fun FloatingBubbleComposable(
        onBubbleClick: () -> Unit
    ) {
        var isDragging by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(60.dp)
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = {
                        Timber.tag("FloatingBubbleService").d("Drag started")
                        isDragging = true
                    }, onDragEnd = {
                        Timber.tag("FloatingBubbleService").d("Drag ended")
                        isDragging = false
                    }, onDrag = { change, dragAmount ->
                        if (isDragging) {
                            change.consume()
                            // Update WindowManager parameters directly during drag
                            updateBubblePositionRealTime(dragAmount.x, dragAmount.y)
                        }
                    })
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 30.dp)
                ) {
                    if (!isDragging) onBubbleClick()
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ), shape = CircleShape
                )
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Notes",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // New method to update position in real-time
    private fun updateBubblePositionRealTime(deltaX: Float, deltaY: Float) {
        floatingBubbleView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams

            // Get screen dimensions
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Calculate new position
            val newX = params.x + deltaX.toInt()
            val newY = params.y + deltaY.toInt()

            // Apply bounds checking to keep bubble on screen
            params.x = newX.coerceIn(0, screenWidth - 180) // 180 is roughly 60dp in pixels
            params.y = newY.coerceIn(0, screenHeight - 180)

            try {
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Timber.tag("FloatingBubbleService").e(e, "Error updating bubble position")
            }
        }
    }

    private fun toggleExpandedView() {
        if (isExpanded) {
            collapseView()
        } else {
            expandView()
        }
    }

    private fun expandView() {
        if (expandedNoteView != null) return

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        expandedNoteView = ComposeView(this)

        // Create a new lifecycle owner for the expanded view each time
        expandedLifecycleOwner = MyLifecycleOwner()
        expandedViewModelStore = ViewModelStore()

        // Set up lifecycle for expanded view
        expandedLifecycleOwner.performRestore(null)
        expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Set lifecycle owners
        expandedNoteView?.setViewTreeLifecycleOwner(expandedLifecycleOwner)
        expandedNoteView?.setViewTreeSavedStateRegistryOwner(expandedLifecycleOwner)
        expandedNoteView?.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore get() = expandedViewModelStore
        })

        // Set content after lifecycle owner is set
        expandedNoteView?.setContent {
            FloatingNoteEditor(noteText = noteText, onNoteChange = {
                noteText = it
                autoSaveNote(it)
            }, onSave = {
                saveNote(noteText)
                showToast("Note saved!")
            }, onMinimize = { collapseView() }, onClose = { stopFloatingBubble() })
        }

        // Add to window manager
        windowManager?.addView(expandedNoteView, params)
        isExpanded = true

        // Hide bubble when expanded
        floatingBubbleView?.visibility = View.GONE
    }

    @Composable
    private fun FloatingNoteEditor(
        noteText: String,
        onNoteChange: (String) -> Unit,
        onSave: () -> Unit,
        onMinimize: () -> Unit,
        onClose: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Notes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = onMinimize, modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onClose, modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = {
                        Text(
                            text = "Start typing your notes...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = Int.MAX_VALUE,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-saving...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onSave, colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }

    private fun collapseView() {
        expandedNoteView?.let {
            windowManager?.removeView(it)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            expandedNoteView = null
        }
        floatingBubbleView?.visibility = View.VISIBLE
        isExpanded = false
    }

    private fun saveNote(content: String) {
        val sharedPref = getSharedPreferences("floating_notes", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("current_note", content)
            apply()
        }
    }

    private fun loadNote() {
        val sharedPref = getSharedPreferences(
            "floating_notes", MODE_PRIVATE
        )
        noteText = sharedPref.getString("current_note", "") ?: ""
    }

    private fun autoSaveNote(content: String) {
        handler.removeCallbacks(autoSaveRunnable)
        autoSaveRunnable = Runnable { saveNote(content) }
        handler.postDelayed(autoSaveRunnable, 1000)
    }

    private fun stopFloatingBubble() {
        floatingBubbleView?.let {
            windowManager?.removeView(it)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        expandedNoteView?.let {
            windowManager?.removeView(it)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        floatingBubbleView = null
        expandedNoteView = null
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        floatingBubbleView?.let {
            windowManager?.removeView(it)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            bubbleLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        expandedNoteView?.let {
            windowManager?.removeView(it)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            expandedLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        floatingBubbleView = null
        expandedNoteView = null
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Floating Notes")
            .setContentText("Tap the floating bubble to add notes")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(pendingIntent)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Floating Bubble", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for floating bubble service"
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var autoSaveRunnable: Runnable
}

// MainActivity.kt - Same as before

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubbleService()
        } else {
            Toast.makeText(this, "Permission required to show floating bubble", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoorTheme {
                MainScreen(
                    onStartFloatingBubble = { checkPermissionAndStartService() },
                    onStopFloatingBubble = { stopFloatingBubbleService() })
            }
        }
    }

    private fun checkPermissionAndStartService() {
        Timber.tag("MainActivity").d("checkPermissionAndStartService called")
        if (Settings.canDrawOverlays(this)) {
            Timber.tag("MainActivity").d("Permission granted, starting service")
            startFloatingBubbleService()
        } else {
            Timber.tag("MainActivity").d("Permission not granted, requesting")
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        Timber.tag("MainActivity").d("requestOverlayPermission called")
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:$packageName".toUri()
        }
        overlayPermissionLauncher.launch(intent)
    }

    private fun startFloatingBubbleService() {
        Timber.tag("MainActivity").d("startFloatingBubbleService called")
        try {
            val intent = Intent(this, FloatingBubbleService::class.java).apply {
                action = FloatingBubbleService.ACTION_START_FLOATING
            }
            Timber.tag("MainActivity").d("Starting foreground service")
            startForegroundService(intent)
            Timber.tag("MainActivity").d("Moving task to back")
            moveTaskToBack(true)
        } catch (e: Exception) {
            Timber.tag("MainActivity").e(e, "Error starting service")
            Toast.makeText(this, "Error starting floating bubble: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun stopFloatingBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_STOP_FLOATING
        }
        startService(intent)
    }
}

@Composable
fun MainScreen(
    onStartFloatingBubble: () -> Unit,
    onStopFloatingBubble: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Floating Notes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Take notes anywhere on your screen",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartFloatingBubble,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Start Floating Bubble", style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onStopFloatingBubble,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Stop Floating Bubble", style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to use",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Tap the bubble to expand and take notes\n• Drag to move it around your screen\n• Notes auto-save as you type",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

