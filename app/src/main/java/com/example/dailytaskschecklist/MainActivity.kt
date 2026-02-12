package com.example.dailytaskschecklist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytaskschecklist.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    Splash,
    AddTask,
    Checklist,
    Options,
    MonthlyStatus,
    About,
    AboutApp,
    Version,
    PrivacyPolicy,
    TermsDisclaimer,
    ContactSupport
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyTasksChecklistTheme {
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val viewModel: TaskViewModel = viewModel(
                    factory = TaskViewModelFactory(database.taskDao(), context.applicationContext)
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskAppContainer(viewModel)
                }
            }
        }
    }
}

@Composable
fun TaskAppContainer(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.Splash) }
    var isFromOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = (context as? ComponentActivity)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.updateDate()
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        delay(2500) 
        currentScreen = if (tasks.isNotEmpty()) {
            Screen.Checklist
        } else {
            isFromOptions = false
            Screen.AddTask
        }
    }

    LaunchedEffect(tasks) {
        if (currentScreen != Screen.Splash) {
            if (tasks.isEmpty() && currentScreen == Screen.Checklist) {
                isFromOptions = false
                currentScreen = Screen.AddTask
            }
        }
    }

    BackHandler(enabled = currentScreen != Screen.Checklist && currentScreen != Screen.Splash) {
        if (tasks.isEmpty() && currentScreen == Screen.AddTask) {
            activity?.finish()
        } else {
            when (currentScreen) {
                Screen.Options -> currentScreen = Screen.Checklist
                Screen.AddTask -> {
                    if (isFromOptions) {
                        currentScreen = Screen.Options
                        isFromOptions = false
                    }
                }
                Screen.MonthlyStatus -> currentScreen = Screen.Options
                Screen.About -> currentScreen = Screen.Options
                Screen.AboutApp, Screen.Version, Screen.PrivacyPolicy, Screen.TermsDisclaimer, Screen.ContactSupport -> {
                    currentScreen = Screen.About
                }
                else -> {}
            }
        }
    }

    when (currentScreen) {
        Screen.Splash -> SplashScreen()
        Screen.AddTask -> {
            AddTaskPage(
                viewModel = viewModel,
                tasks = tasks,
                showBackIcon = isFromOptions,
                onBack = { 
                    currentScreen = Screen.Options
                    isFromOptions = false
                },
                onDone = { 
                    if (isFromOptions) {
                        currentScreen = Screen.Options
                    } else {
                        currentScreen = Screen.Checklist 
                    }
                    isFromOptions = false
                }
            )
        }
        Screen.Checklist -> {
            ChecklistPage(
                viewModel = viewModel,
                tasks = tasks,
                onOpenOptions = { currentScreen = Screen.Options }
            )
        }
        Screen.Options -> {
            OptionsPage(
                onNavigateToManage = { 
                    isFromOptions = true
                    currentScreen = Screen.AddTask 
                },
                onNavigateToMonthlyStatus = { currentScreen = Screen.MonthlyStatus },
                onNavigateToAbout = { currentScreen = Screen.About },
                onBack = { currentScreen = Screen.Checklist }
            )
        }
        Screen.MonthlyStatus -> {
            MonthlyStatusPage(
                viewModel = viewModel,
                tasks = tasks,
                onBack = { currentScreen = Screen.Options }
            )
        }
        Screen.About -> {
            AboutPage(
                onNavigateToAboutApp = { currentScreen = Screen.AboutApp },
                onNavigateToVersion = { currentScreen = Screen.Version },
                onNavigateToPrivacy = { currentScreen = Screen.PrivacyPolicy },
                onNavigateToTerms = { currentScreen = Screen.TermsDisclaimer },
                onNavigateToContact = { currentScreen = Screen.ContactSupport },
                onBack = { currentScreen = Screen.Options }
            )
        }
        Screen.AboutApp -> {
            SubAboutPage(title = "About the App", onBack = { currentScreen = Screen.About }) {
                Text(
                    text = "Tasks Checklist is a simple and easy-to-use app designed to help you stay organized and productive every day. It allows you to create, track, and complete your daily tasks with clarity and consistency.\n\n" +
                           "The app focuses on simplicity — no unnecessary features, no distractions. You can define your tasks, mark them as completed as you progress through the day, and review your daily or monthly completion status to understand your habits better.\n\n" +
                           "Tasks Checklist is ideal for:\n" +
                           "• Managing personal daily routines\n" +
                           "• Tracking work or study-related tasks\n" +
                           "• Building healthy habits\n" +
                           "• Ensuring nothing important is forgotten\n\n" +
                           "All data is stored locally on your device, ensuring privacy and fast access without requiring an internet connection. The app does not collect or share personal data.\n\n" +
                           "Our goal is to provide a lightweight, reliable checklist experience that helps you focus on what matters most — getting things done, one task at a time.",
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
        Screen.Version -> {
            SubAboutPage(title = "Version", onBack = { currentScreen = Screen.About }) {
                Text("App Name: Tasks Checklist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Version: 1.0.6", fontSize = 16.sp)
            }
        }
        Screen.PrivacyPolicy -> {
            SubAboutPage(title = "Privacy Policy", onBack = { currentScreen = Screen.About }) {
                Text(
                    text = "Last updated: 3rd February 2026\n\n" +
                           "Tasks Checklist respects your privacy and is committed to protecting it. This Privacy Policy explains how the app handles information when you use it.\n\n" +
                           "Information Collection\n" +
                           "Tasks Checklist does not collect, store, or transmit any personal information. All tasks, checklist data, and completion history are stored locally on your device only.\n\n" +
                           "The app does not require you to create an account, log in, or provide personal details such as name, email address, phone number, or location.\n\n" +
                           "Data Storage\n" +
                           "All task data is saved locally on your device using internal app storage. The app does not use cloud storage or external servers. The developer has no access to your tasks or usage data.\n\n" +
                           "If you uninstall the app, clear app data, or reset your device, all stored data will be permanently deleted. This data cannot be recovered.\n\n" +
                           "Internet Usage\n" +
                           "Tasks Checklist works fully offline. It does not require an internet connection to function and does not transmit data over the network.\n\n" +
                           "Third-Party Services\n" +
                           "The app does not integrate with:\n" +
                           "• Advertising networks\n" +
                           "• Analytics tools\n" +
                           "• Social media platforms\n" +
                           "• Third-party SDKs that collect personal data\n\n" +
                           "If such services are added in future versions, this Privacy Policy will be updated accordingly.\n\n" +
                           "Children’s Privacy\n" +
                           "Tasks Checklist does not knowingly collect any personal information from children under the age of 13. Since no personal data is collected at all, the app is safe for general use.\n\n" +
                           "Data Security\n" +
                           "Because all data is stored locally on your device, data security depends on your device’s security settings. We recommend using standard device protections such as screen locks and secure storage options.\n\n" +
                           "Changes to This Privacy Policy\n" +
                           "This Privacy Policy may be updated from time to time. Any changes will be reflected within the app. Continued use of the app after changes indicates acceptance of the updated policy.\n\n" +
                           "Contact\n" +
                           "If you have questions or concerns about this Privacy Policy, you may contact the developer through the contact details provided on the app store listing.",
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
        Screen.TermsDisclaimer -> {
            SubAboutPage(title = "Terms & Disclaimer", onBack = { currentScreen = Screen.About }) {
                Text(
                    text = "Tasks Checklist is provided as a simple task tracking and checklist application. By installing or using this app, you agree to the terms and conditions outlined below.\n\n" +
                           "Intended Use\n" +
                           "Tasks Checklist is intended solely as a tool to help users track and mark their daily tasks in a checklist format.\n\n" +
                           "User Responsibility\n" +
                           "Users are fully responsible for:\n" +
                           "• Entering correct task names and schedules\n" +
                           "• Verifying that tasks are completed correctly and on time\n\n" +
                           "The app should not be relied upon as the sole method for managing routines.\n\n" +
                           "App Availability & Bugs\n" +
                           "The app is provided on an “as-is” and “as-available” basis. While reasonable efforts are made to ensure proper functioning, the developer does not guarantee that the app will be error-free, uninterrupted, or free from bugs. The app may occasionally fail due to software issues, device limitations, operating system updates, or other technical reasons.\n\n" +
                           "Limitation of Liability\n" +
                           "To the fullest extent permitted by law, the developer shall not be held liable for any direct, indirect, incidental, or consequential damages, including but not limited to missed tasks, data loss, or other losses arising from the use of or inability to use this app.\n\n" +
                           "Data & Storage\n" +
                           "All data entered into Tasks Checklist is stored locally on the user’s device only. The app does not transmit data to external servers or cloud services.\n\n" +
                           "Data Deletion & Storage Responsibility\n" +
                           "Deleting app data, clearing storage, uninstalling the app, using storage-cleaning tools, or performing device resets will permanently remove all stored information. The developer is not responsible for any data loss resulting from user actions, device settings, system updates, or storage management tools.\n\n" +
                           "Changes to the App\n" +
                           "The developer reserves the right to modify, update, or discontinue any part of the app at any time without prior notice.\n\n" +
                           "Acceptance of Terms\n" +
                           "By installing or using Tasks Checklist, you acknowledge that you have read, understood, and agreed to these Terms & Disclaimer.",
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
        Screen.ContactSupport -> {
            SubAboutPage(title = "Contact Support", onBack = { currentScreen = Screen.About }) {
                Text(
                    text = "If you have any questions, feedback, or need assistance, we’re here to help.\n\n" +
                           "You can reach out to us for:\n" +
                           "• Reporting bugs or technical issues\n" +
                           "• Suggesting new features\n" +
                           "• General inquiries about the app\n\n" +
                           "You can contact us at:\n" +
                           "developer.checklist@gmail.com\n\n" +
                           "You may also reach out through the developer information provided on the app’s store listing. We appreciate your support and aim to respond as quickly as possible.",
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCBE6FF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(pulseScale)
            ) {
                Surface(
                    modifier = Modifier.size(220.dp),
                    shape = RoundedCornerShape(48.dp),
                    color = Color.Transparent,
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF36D1FF), Color(0xFF0077FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_tasks),
                            contentDescription = null, // Decorative icon
                            modifier = Modifier.fillMaxSize().scale(1.5f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Tasks Checklist",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = BluePrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simple daily task tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BluePrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Track • Take • Thrive",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = BluePrimary.copy(alpha = 0.6f),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(64.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .clip(CircleShape),
                color = BluePrimary,
                trackColor = BluePrimary.copy(alpha = 0.1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskPage(
    viewModel: TaskViewModel,
    tasks: List<Task>,
    showBackIcon: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            CenterAlignedTopAppBar(
                title = { Text("Manage Tasks", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    if (showBackIcon) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = TextHeader)
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onDone,
                        enabled = tasks.isNotEmpty()
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            color = if (tasks.isNotEmpty()) BluePrimary else Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add task icon") },
                text = { Text("Add Task") },
                containerColor = BluePrimary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (tasks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.AddCircle,
                    message = "Your task list is empty.\nTap the button below to add your first task."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks) { task ->
                        TaskEditCard(
                            task = task,
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, tablets, times, priority ->
                    scope.launch {
                        val isAdded = viewModel.addTask(name, tablets, times, priority)
                        if (isAdded) {
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Task '$name' already exists!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text("Delete Task", color = TextHeader) },
                text = { Text("Are you sure you want to delete ${taskToDelete?.name}?", color = TextPrimary) },
                confirmButton = {
                    Button(
                        onClick = {
                            taskToDelete?.let { viewModel.deleteTask(it) }
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistPage(
    viewModel: TaskViewModel,
    tasks: List<Task>,
    onOpenOptions: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Morning", "Afternoon", "Evening")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTabTitle = tabs[selectedTabIndex]
    
    val currentDateStr by viewModel.currentDate.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val isSaved = todayRecords.any { it.timeSlot == currentTabTitle }

    val morningCompleted = todayRecords.any { it.timeSlot == "Morning" }
    val afternoonCompleted = todayRecords.any { it.timeSlot == "Afternoon" }
    val eveningCompleted = todayRecords.any { it.timeSlot == "Evening" }

    val currentItems = remember(tasks, todayRecords, selectedTabIndex) {
        val list = if (isSaved) {
            todayRecords.filter { it.timeSlot == currentTabTitle }.map { record ->
                Task(
                    name = record.taskName,
                    numberOfTablets = record.taskDetails,
                    times = record.timeSlot,
                    priority = record.taskPriority,
                    taskTaken = record.wasTaken
                )
            }
        } else {
            tasks.filter { it.times.contains(currentTabTitle) }
        }
        
        list.sortedByDescending { task ->
            when (task.priority) {
                "High" -> 3
                "Medium" -> 2
                "Low" -> 1
                else -> 0
            }
        }
    }

    val takenCount = remember(currentItems, selectedTabIndex, isSaved) {
        currentItems.count { item ->
            if (isSaved) {
                item.taskTaken
            } else {
                when (currentTabTitle) {
                    "Morning" -> item.isTakenMorning
                    "Afternoon" -> item.isTakenAfternoon
                    "Evening" -> item.isTakenEvening
                    else -> false
                }
            }
        }
    }

    var showPartialSaveDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val rotation = animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(tween(1000, easing = LinearEasing))
        } else {
            tween(0)
        },
        label = "refreshRotation"
    )

    fun formatUIDate(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            val calendar = Calendar.getInstance()
            calendar.time = date!!
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val suffix = when (day) {
                1, 21, 31 -> "st"
                2, 22 -> "nd"
                3, 23 -> "rd"
                else -> "th"
            }
            val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
            "$day$suffix $monthYear"
        } catch (e: Exception) {
            dateStr
        }
    }

    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            CenterAlignedTopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            isRefreshing = true
                            val didChange = viewModel.updateDate()
                            delay(1000)
                            isRefreshing = false
                            if (didChange) {
                                Toast.makeText(context, "Data updated for the new day!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Already up to date", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh data",
                            tint = TextHeader.copy(alpha = 0.7f),
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                    IconButton(onClick = onOpenOptions) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open options menu",
                            tint = TextHeader.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = { 
                        if (takenCount == 0) {
                            Toast.makeText(context, "Please select a task", Toast.LENGTH_SHORT).show()
                        } else if (takenCount < currentItems.size) {
                            showPartialSaveDialog = true
                        } else {
                            viewModel.saveDailyRecords(currentItems, currentTabTitle)
                            Toast.makeText(context, "$currentTabTitle records saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaved && currentItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color.Gray else BluePrimary,
                        contentColor = Color.White,
                        disabledContainerColor = if (isSaved) Color.Gray else BluePrimary.copy(alpha = 0.38f),
                        disabledContentColor = Color.White.copy(alpha = 0.74f)
                    )
                ) {
                    Icon(if (isSaved) Icons.Default.DoneAll else Icons.Default.Save, contentDescription = null) // Descriptive text already provided by button label
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isSaved) "$currentTabTitle Records Saved" else "Save $currentTabTitle Tasks",
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Column {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "Tasks Checklist for",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = TextHeader
                    )
                    @Suppress("DEPRECATION")
                    Text(
                        text = formatUIDate(currentDateStr),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = TextHeader
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomTabButton(
                    text = "Morning",
                    isSelected = selectedTabIndex == 0,
                    isCompleted = morningCompleted,
                    onClick = { selectedTabIndex = 0 },
                    bgColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = "Afternoon",
                    isSelected = selectedTabIndex == 1,
                    isCompleted = afternoonCompleted,
                    onClick = { selectedTabIndex = 1 },
                    bgColor = Color(0xFFF1F8E9),
                    textColor = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = "Evening",
                    isSelected = selectedTabIndex == 2,
                    isCompleted = eveningCompleted,
                    onClick = { selectedTabIndex = 2 },
                    bgColor = Color(0xFFFBE9E7),
                    textColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            @Suppress("DEPRECATION")
            Text(
                text = "Total number of tasks : ${currentItems.size}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = TextHeader
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.DoneAll,
                        message = "No tasks scheduled for $currentTabTitle."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(currentItems) { task ->
                        val isTaken = if (isSaved) {
                            task.taskTaken
                        } else {
                            when (currentTabTitle) {
                                "Morning" -> task.isTakenMorning
                                "Afternoon" -> task.isTakenAfternoon
                                "Evening" -> task.isTakenEvening
                                else -> false
                            }
                        }
                        TaskChecklistCard(
                            task = task,
                            isTaken = isTaken,
                            onToggle = { if (!isSaved) viewModel.toggleTaskTaken(task, currentTabTitle) }
                        )
                    }
                }
            }
        }

        if (showPartialSaveDialog) {
            AlertDialog(
                onDismissRequest = { showPartialSaveDialog = false },
                title = { Text("Save Tasks") },
                text = { Text("Do you want to save without selecting all the tasks?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.saveDailyRecords(currentItems, currentTabTitle)
                        Toast.makeText(context, "$currentTabTitle records saved!", Toast.LENGTH_SHORT).show()
                        showPartialSaveDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPartialSaveDialog = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun CustomTabButton(
    text: String,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Color(0xFF388E3C) else Color(0xFFD32F2F))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(if (isSelected) 2.dp else 1.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .background(bgColor.copy(alpha = if (isSelected) 1f else 0.3f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = text,
                    color = if (isSelected) textColor else textColor.copy(alpha = 0.7f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(textColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsPage(
    onNavigateToManage: () -> Unit,
    onNavigateToMonthlyStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            LargeTopAppBar(
                title = { Text("Options", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = TextHeader)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OptionButton(
                text = "Monthly Progress",
                icon = Icons.Default.CalendarMonth,
                color = BluePrimary,
                onClick = onNavigateToMonthlyStatus
            )
            OptionButton(
                text = "Manage Tasks",
                icon = Icons.Default.AddCircle,
                color = BluePrimary,
                onClick = onNavigateToManage
            )
            OptionButton(
                text = "About",
                icon = Icons.Default.Info,
                color = BluePrimary,
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // Descriptive text already provided by adjacent Text
                    modifier = Modifier.padding(12.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            @Suppress("DEPRECATION")
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatusPage(viewModel: TaskViewModel, tasks: List<Task>, onBack: () -> Unit) {
    var selectedMonthIndex by remember { mutableStateOf(0) } 
    val currentMonthRecords by viewModel.getRecordsForMonth(0).collectAsState(initial = emptyList())
    val previousMonthRecords by viewModel.getRecordsForMonth(-1).collectAsState(initial = emptyList())

    val records = if (selectedMonthIndex == 0) currentMonthRecords else previousMonthRecords
    
    val calendar = Calendar.getInstance()
    if (selectedMonthIndex == 1) calendar.add(Calendar.MONTH, -1)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            CenterAlignedTopAppBar(
                title = { Text("Monthly Progress", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = TextHeader)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusTabButton(
                    text = "Current Month",
                    isSelected = selectedMonthIndex == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMonthIndex = 0 }
                )
                StatusTabButton(
                    text = "Previous Month",
                    isSelected = selectedMonthIndex == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMonthIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            @Suppress("DEPRECATION")
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextHeader,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BluePrimary)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHeaderCell("Date", Modifier.weight(1.2f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell("Morning", Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell("Afternoon", Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell("Evening", Modifier.weight(1f))
                    }

                    val dates = remember(selectedMonthIndex) { 
                        getDatesForMonth(if (selectedMonthIndex == 0) 0 else -1) 
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dates) { date ->
                            StatusRow(date, records, tasks)
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BluePrimary else Color.White,
        label = "backgroundColorAnim"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else BluePrimary,
        label = "contentColorAnim"
    )

    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = if (!isSelected) BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 14.sp
    )
}

fun formatReportDate(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val suffix = when (day) {
        1, 21, 31 -> "st"
        2, 22 -> "nd"
        3, 23 -> "rd"
        else -> "th"
    }
    val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
    return "$day$suffix $monthYear"
}

@Composable
fun StatusRow(date: Date, records: List<TaskRecord>, tasks: List<Task>) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    val displayDate = formatReportDate(date)
    
    val rowRecords = records.filter { it.date == dateStr }
    
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(displayDate, modifier = Modifier.weight(1.2f).padding(vertical = 12.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary)
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Morning", date, rowRecords, tasks))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Afternoon", date, rowRecords, tasks))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Evening", date, rowRecords, tasks))
    }
}

@Composable
fun StatusCell(modifier: Modifier, status: StatusType) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (status) {
            StatusType.Taken -> Icon(Icons.Default.CheckBox, contentDescription = "Task completed", tint = Color(0xFF388E3C), modifier = Modifier.size(24.dp))
            StatusType.None -> Text("-", color = Color.LightGray, fontWeight = FontWeight.Bold)
            StatusType.Future -> { /* Empty */ }
            else -> { /* No Cross used */ }
        }
    }
}

enum class StatusType { Taken, Missed, None, Future }

fun getStatus(slot: String, date: Date, records: List<TaskRecord>, tasks: List<Task>): StatusType {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    if (date.after(today.time)) return StatusType.Future

    val record = records.find { it.timeSlot == slot }
    if (record != null) return StatusType.Taken

    return if (date.before(today.time)) {
        val isScheduled = tasks.any { it.times.contains(slot) }
        if (isScheduled) StatusType.None else StatusType.Future
    } else {
        // Today
        StatusType.Future
    }
}

fun getDatesForMonth(monthOffset: Int): List<Date> {
    val dates = mutableListOf<Date>()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    
    cal.add(Calendar.MONTH, monthOffset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val month = cal.get(Calendar.MONTH)
    while (cal.get(Calendar.MONTH) == month) {
        dates.add(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dates
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateToAboutApp: () -> Unit,
    onNavigateToVersion: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToContact: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            LargeTopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = TextHeader)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AboutOptionItem(text = "About the App", onClick = onNavigateToAboutApp)
            AboutOptionItem(text = "Version", onClick = onNavigateToVersion)
            AboutOptionItem(text = "Privacy Policy", onClick = onNavigateToPrivacy)
            AboutOptionItem(text = "Terms & Disclaimer", onClick = onNavigateToTerms)
            AboutOptionItem(text = "Contact Support", onClick = onNavigateToContact)
        }
    }
}

@Composable
fun AboutOptionItem(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null, // Purely decorative
                tint = Color.LightGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAboutPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            @Suppress("DEPRECATION")
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = TextHeader)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }
    }
}

@Composable
fun TaskEditCard(task: Task, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBE6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null, // Decorative icon
                    tint = BluePrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${task.numberOfTablets} • ${task.times} • ${task.priority}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TaskChecklistCard(task: Task, isTaken: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isTaken) 
            Color(0xFFEEF6FB) 
        else 
            Color.White,
        label = "backgroundColorAnim"
    )

    val priorityColor = when (task.priority) {
        "Low" -> Color(0xFF388E3C)
        "Medium" -> Color(0xFFFBC02D)
        "High" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = if (isTaken) BorderStroke(1.dp, Color(0xFFC7E3F4)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTaken,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = BluePrimary,
                    uncheckedColor = TextSecondary
                )
            )
            
            // Priority Standing Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isTaken) TextSecondary else TextPrimary
                    )
                )
                Text(
                    text = task.numberOfTablets,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative background icon
            modifier = Modifier.size(84.dp),
            tint = BluePrimary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var tablets by remember { mutableStateOf("") }
    val timeOptions = listOf("Morning", "Afternoon", "Evening")
    val priorityOptions = listOf("Low", "Medium", "High")
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }
    var selectedPriority by remember { mutableStateOf("Medium") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            @Suppress("DEPRECATION")
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "New Task",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeader
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Task Name") },
                        placeholder = { Text("e.g. Drink Water") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = tablets,
                        onValueChange = { tablets = it },
                        label = { Text("Details") },
                        placeholder = { Text("e.g. 500ml") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                }

                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeader
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorityOptions.forEach { priority ->
                            val isSelected = selectedPriority == priority
                            val chipColor = when(priority) {
                                "Low" -> if (isSelected) Color(0xFF388E3C) else Color(0xFFF1F8E9)
                                "Medium" -> if (isSelected) Color(0xFFFBC02D) else Color(0xFFFFF9C4)
                                "High" -> if (isSelected) Color(0xFFD32F2F) else Color(0xFFFBE9E7)
                                else -> BluePrimary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipColor)
                                    .clickable {
                                        selectedPriority = priority
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priority,
                                    color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.8f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeader
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeOptions.forEach { time ->
                            val isSelected = selectedTimes.contains(time)
                            val chipColor = when(time) {
                                "Morning" -> if (isSelected) Color(0xFF1976D2) else Color(0xFFE3F2FD)
                                "Afternoon" -> if (isSelected) Color(0xFF388E3C) else Color(0xFFF1F8E9)
                                "Evening" -> if (isSelected) Color(0xFFD32F2F) else Color(0xFFFBE9E7)
                                else -> BluePrimary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipColor)
                                    .clickable {
                                        selectedTimes = if (isSelected) selectedTimes - time else selectedTimes + time
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = time,
                                    color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.8f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onConfirm(name, tablets, selectedTimes.joinToString(", "), selectedPriority) 
                        },
                        enabled = name.isNotBlank() && tablets.isNotBlank() && selectedTimes.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Add to List", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}