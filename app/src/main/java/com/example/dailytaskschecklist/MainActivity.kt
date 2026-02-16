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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            val context = LocalContext.current
            val database = AppDatabase.getDatabase(context)
            val viewModel: TaskViewModel = viewModel(
                factory = TaskViewModelFactory(database.taskDao(), context.applicationContext)
            )
            val themePreference by viewModel.themePreference.collectAsState()

            DailyTasksChecklistTheme(themePreference = themePreference) {
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
        Screen.Splash -> SplashScreen(viewModel)
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
                viewModel = viewModel,
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
            SubAboutPage(title = stringResource(R.string.about_the_app), onBack = { currentScreen = Screen.About }) {
                Text(
                    text = stringResource(R.string.about_app_content),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Screen.Version -> {
            SubAboutPage(title = stringResource(R.string.version), onBack = { currentScreen = Screen.About }) {
                Text(stringResource(R.string.version_name), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.version_number), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Screen.PrivacyPolicy -> {
            SubAboutPage(title = stringResource(R.string.privacy_policy), onBack = { currentScreen = Screen.About }) {
                Text(
                    text = stringResource(R.string.privacy_policy_content_1) + "\n\n" +
                           stringResource(R.string.privacy_policy_content_2) + "\n\n" +
                           stringResource(R.string.privacy_policy_content_3),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Screen.TermsDisclaimer -> {
            SubAboutPage(title = stringResource(R.string.terms_disclaimer), onBack = { currentScreen = Screen.About }) {
                Text(
                    text = stringResource(R.string.terms_disclaimer_content_1) + "\n\n" +
                           stringResource(R.string.terms_disclaimer_content_2) + "\n\n" +
                           stringResource(R.string.terms_disclaimer_content_3),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Screen.ContactSupport -> {
            SubAboutPage(title = stringResource(R.string.contact_support), onBack = { currentScreen = Screen.About }) {
                Text(
                    text = stringResource(R.string.contact_support_content),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun SplashScreen(viewModel: TaskViewModel) {
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

    val themePref = viewModel.themePreference.collectAsState().value
    val isDark = themePref == ThemePreference.Dark || (themePref == ThemePreference.Default && isSystemInDarkTheme())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) AppBackgroundDark else AppBackground),
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
                text = stringResource(R.string.splash_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = if (isDark) BluePrimaryDark else BluePrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = (if (isDark) BluePrimaryDark else BluePrimary).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = (if (isDark) BluePrimaryDark else BluePrimary).copy(alpha = 0.6f),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(64.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .clip(CircleShape),
                color = if (isDark) BluePrimaryDark else BluePrimary,
                trackColor = (if (isDark) BluePrimaryDark else BluePrimary).copy(alpha = 0.1f)
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
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.manage_tasks), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    if (showBackIcon) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    val themePref = viewModel.themePreference.collectAsState().value
                    val isDark = themePref == ThemePreference.Dark || (themePref == ThemePreference.Default && isSystemInDarkTheme())
                    TextButton(
                        onClick = onDone,
                        enabled = tasks.isNotEmpty()
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            fontWeight = FontWeight.Bold,
                            color = if (tasks.isNotEmpty()) (if (isDark) BluePrimaryDark else BluePrimary) else Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_task_icon)) },
                text = { Text(stringResource(R.string.add_task)) },
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
                    message = stringResource(R.string.empty_task_list)
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
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, tablets, times, priority ->
                    scope.launch {
                        val isAdded = viewModel.addTask(name, tablets, times, priority)
                        if (isAdded) {
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, context.getString(R.string.task_already_exists, name), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (taskToDelete != null) {
            val themePref = viewModel.themePreference.collectAsState().value
            val isDark = themePref == ThemePreference.Dark || (themePref == ThemePreference.Default && isSystemInDarkTheme())
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text(stringResource(R.string.delete_task_title), color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black) },
                text = { Text(stringResource(R.string.delete_task_confirmation, taskToDelete?.name ?: ""), color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black) },
                confirmButton = {
                    Button(
                        onClick = {
                            taskToDelete?.let { viewModel.deleteTask(it) }
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text(stringResource(R.string.cancel), color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
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
    val tabs = TimeSlot.entries
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTab = tabs[selectedTabIndex]
    
    val currentDateStr by viewModel.currentDate.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val isSaved = todayRecords.any { it.timeSlot == currentTab.title }

    val morningCompleted = todayRecords.any { it.timeSlot == TimeSlot.Morning.title }
    val afternoonCompleted = todayRecords.any { it.timeSlot == TimeSlot.Afternoon.title }
    val eveningCompleted = todayRecords.any { it.timeSlot == TimeSlot.Evening.title }

    val themePref = viewModel.themePreference.collectAsState().value
    val isDark = themePref == ThemePreference.Dark || (themePref == ThemePreference.Default && isSystemInDarkTheme())

    val currentItems = remember(tasks, todayRecords, selectedTabIndex) {
        val list = if (isSaved) {
            todayRecords.filter { it.timeSlot == currentTab.title }.map { record ->
                Task(
                    name = record.taskName,
                    numberOfTablets = record.taskDetails,
                    times = listOf(currentTab),
                    priority = record.taskPriority,
                    taskTaken = record.wasTaken
                )
            }
        } else {
            tasks.filter { it.times.contains(currentTab) }
        }
        
        list.sortedByDescending { task ->
            when (task.priority) {
                Priority.High.title -> 3
                Priority.Medium.title -> 2
                Priority.Low.title -> 1
                else -> 0
            }
        }
    }

    val takenCount = remember(currentItems, selectedTabIndex, isSaved) {
        currentItems.count { item ->
            if (isSaved) {
                item.taskTaken
            } else {
                when (currentTab) {
                    TimeSlot.Morning -> item.isTakenMorning
                    TimeSlot.Afternoon -> item.isTakenAfternoon
                    TimeSlot.Evening -> item.isTakenEvening
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
                                Toast.makeText(context, context.getString(R.string.data_updated), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.already_up_to_date), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_data),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                    IconButton(onClick = onOpenOptions) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.open_options_menu),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                            Toast.makeText(context, context.getString(R.string.please_select_a_task), Toast.LENGTH_SHORT).show()
                        } else if (takenCount < currentItems.size) {
                            showPartialSaveDialog = true
                        } else {
                            viewModel.saveDailyRecords(currentItems, currentTab)
                            Toast.makeText(context, context.getString(R.string.records_saved, currentTab.title), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaved && currentItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color.Gray else (if (isDark) BluePrimaryDark else BluePrimary),
                        contentColor = if (isDark) Color.Black else Color.White,
                        disabledContainerColor = if (isSaved) Color.Gray else (if (isDark) BluePrimaryDark else BluePrimary).copy(alpha = 0.38f),
                        disabledContentColor = (if (isDark) Color.Black else Color.White).copy(alpha = 0.74f)
                    )
                ) {
                    Icon(if (isSaved) Icons.Default.DoneAll else Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isSaved) stringResource(R.string.records_saved_for_timeslot, currentTab.title) else stringResource(R.string.save_tasks_for_timeslot, currentTab.title),
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
                    Text(
                        text = stringResource(R.string.checklist_for),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = formatUIDate(currentDateStr),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
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
                    text = TimeSlot.Morning.title,
                    isSelected = selectedTabIndex == 0,
                    isCompleted = morningCompleted,
                    onClick = { selectedTabIndex = 0 },
                    bgColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFE3F2FD),
                    textColor = if (isDark) MorningBlueDark else Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = TimeSlot.Afternoon.title,
                    isSelected = selectedTabIndex == 1,
                    isCompleted = afternoonCompleted,
                    onClick = { selectedTabIndex = 1 },
                    bgColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF1F8E9),
                    textColor = if (isDark) AfternoonGreenDark else Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = TimeSlot.Evening.title,
                    isSelected = selectedTabIndex == 2,
                    isCompleted = eveningCompleted,
                    onClick = { selectedTabIndex = 2 },
                    bgColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFBE9E7),
                    textColor = if (isDark) EveningRedDark else Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.total_tasks, currentItems.size),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.DoneAll,
                        message = stringResource(R.string.no_tasks_for_timeslot, currentTab.title)
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
                            when (currentTab) {
                                TimeSlot.Morning -> task.isTakenMorning
                                TimeSlot.Afternoon -> task.isTakenAfternoon
                                TimeSlot.Evening -> task.isTakenEvening
                            }
                        }
                        TaskChecklistCard(
                            task = task,
                            isTaken = isTaken,
                            onToggle = { if (!isSaved) viewModel.toggleTaskTaken(task, currentTab) },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        if (showPartialSaveDialog) {
            AlertDialog(
                onDismissRequest = { showPartialSaveDialog = false },
                title = { Text(stringResource(R.string.save_tasks), color = MaterialTheme.colorScheme.onSurface) },
                text = { Text(stringResource(R.string.save_partial_records_dialog), color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.saveDailyRecords(currentItems, currentTab)
                        Toast.makeText(context, context.getString(R.string.records_saved, currentTab.title), Toast.LENGTH_SHORT).show()
                        showPartialSaveDialog = false
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPartialSaveDialog = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
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
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
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
    viewModel: TaskViewModel,
    onNavigateToManage: () -> Unit,
    onNavigateToMonthlyStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    val themePreference by viewModel.themePreference.collectAsState()
    val isDark = isSystemInDarkTheme() || themePreference == ThemePreference.Dark

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.options), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
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
                text = stringResource(R.string.monthly_progress),
                icon = Icons.Default.CalendarMonth,
                color = BluePrimary,
                onClick = onNavigateToMonthlyStatus,
                isDark = isDark
            )
            OptionButton(
                text = stringResource(R.string.manage_tasks),
                icon = Icons.Default.AddCircle,
                color = BluePrimary,
                onClick = onNavigateToManage,
                isDark = isDark
            )
            OptionButton(
                text = stringResource(R.string.theme),
                icon = Icons.Default.Palette,
                color = BluePrimary,
                onClick = { showThemeDialog = true },
                isDark = isDark
            )
            OptionButton(
                text = stringResource(R.string.about),
                icon = Icons.Default.Info,
                color = BluePrimary,
                onClick = onNavigateToAbout,
                isDark = isDark
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text(stringResource(R.string.select_theme), color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black) },
                text = {
                    Column {
                        ThemeOptionItem(
                            text = stringResource(R.string.light_mode),
                            isSelected = themePreference == ThemePreference.Light,
                            onClick = {
                                viewModel.setThemePreference(ThemePreference.Light)
                                showThemeDialog = false
                            },
                            isDark = isDark
                        )
                        ThemeOptionItem(
                            text = stringResource(R.string.dark_mode),
                            isSelected = themePreference == ThemePreference.Dark,
                            onClick = {
                                viewModel.setThemePreference(ThemePreference.Dark)
                                showThemeDialog = false
                            },
                            isDark = isDark
                        )
                        ThemeOptionItem(
                            text = stringResource(R.string.default_mode),
                            isSelected = themePreference == ThemePreference.Default,
                            onClick = {
                                viewModel.setThemePreference(ThemePreference.Default)
                                showThemeDialog = false
                            },
                            isDark = isDark
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text(stringResource(R.string.cancel), color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun ThemeOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected, 
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = BluePrimary,
                unselectedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black)
    }
}

@Composable
fun OptionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
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
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.monthly_progress), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    text = stringResource(R.string.current_month),
                    isSelected = selectedMonthIndex == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMonthIndex = 0 }
                )
                StatusTabButton(
                    text = stringResource(R.string.previous_month),
                    isSelected = selectedMonthIndex == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMonthIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        TableHeaderCell(stringResource(R.string.date), Modifier.weight(1.2f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell(TimeSlot.Morning.title, Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell(TimeSlot.Afternoon.title, Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell(TimeSlot.Evening.title, Modifier.weight(1f))
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
        targetValue = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
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
        Text(displayDate, modifier = Modifier.weight(1.2f).padding(vertical = 12.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus(TimeSlot.Morning, date, rowRecords, tasks))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus(TimeSlot.Afternoon, date, rowRecords, tasks))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus(TimeSlot.Evening, date, rowRecords, tasks))
    }
}

@Composable
fun StatusCell(modifier: Modifier, status: StatusType) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (status) {
            StatusType.Taken -> Icon(Icons.Default.CheckBox, contentDescription = stringResource(R.string.task_completed), tint = Color(0xFF388E3C), modifier = Modifier.size(24.dp))
            StatusType.None -> Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            StatusType.Future -> { /* Empty */ }
            else -> { /* No Cross used */ }
        }
    }
}

enum class StatusType { Taken, Missed, None, Future }

fun getStatus(slot: TimeSlot, date: Date, records: List<TaskRecord>, tasks: List<Task>): StatusType {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    if (date.after(today.time)) return StatusType.Future

    val record = records.find { it.timeSlot == slot.title }
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
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
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
            AboutOptionItem(text = stringResource(R.string.about_the_app), onClick = onNavigateToAboutApp)
            AboutOptionItem(text = stringResource(R.string.version), onClick = onNavigateToVersion)
            AboutOptionItem(text = stringResource(R.string.privacy_policy), onClick = onNavigateToPrivacy)
            AboutOptionItem(text = stringResource(R.string.terms_disclaimer), onClick = onNavigateToTerms)
            AboutOptionItem(text = stringResource(R.string.contact_support), onClick = onNavigateToContact)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null, 
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
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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
            containerColor = MaterialTheme.colorScheme.surface
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
                    contentDescription = null,
                    tint = BluePrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.task_details_format, task.numberOfTablets, task.times.joinToString(", ") { it.title }, task.priority),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFFB71C1C)
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_task_desc), tint = Color(0xFFB71C1C))
            }
        }
    }
}

@Composable
fun TaskChecklistCard(task: Task, isTaken: Boolean, onToggle: () -> Unit, isDark: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isTaken) {
            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFEEF6FB)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColorAnim"
    )

    val priorityColor = when (task.priority) {
        Priority.Low.title -> if (isDark) LowPriorityDark else Color(0xFF388E3C)
        Priority.Medium.title -> if (isDark) MediumPriorityDark else Color(0xFFFBC02D)
        Priority.High.title -> if (isDark) HighPriorityDark else Color(0xFFD32F2F)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isTaken) BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFC7E3F4)) else null
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
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
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
                        color = if (isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = task.numberOfTablets,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            contentDescription = null,
            modifier = Modifier.size(84.dp),
            tint = BluePrimary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddTaskDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<TimeSlot>, Priority) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var tablets by remember { mutableStateOf("") }
    val timeOptions = TimeSlot.entries
    val priorityOptions = Priority.entries
    var selectedTimes by remember { mutableStateOf(setOf<TimeSlot>()) }
    var selectedPriority by remember { mutableStateOf(Priority.Medium) }
    
    val themePref = viewModel.themePreference.collectAsState().value
    val isDark = themePref == ThemePreference.Dark || (themePref == ThemePreference.Default && isSystemInDarkTheme())

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.new_task),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.task_name_label)) },
                        placeholder = { Text(stringResource(R.string.task_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = tablets,
                        onValueChange = { tablets = it },
                        label = { Text(stringResource(R.string.details_label)) },
                        placeholder = { Text(stringResource(R.string.details_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                }

                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.priority),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorityOptions.forEach { priority ->
                            val isSelected = selectedPriority == priority
                            val chipColor = when(priority) {
                                Priority.Low -> if (isSelected) (if (isDark) LowPriorityDark else Color(0xFF388E3C)) else (if (isDark) Color(0xFF2E3B2E) else Color(0xFFF1F8E9))
                                Priority.Medium -> if (isSelected) (if (isDark) MediumPriorityDark else Color(0xFFFBC02D)) else (if (isDark) Color(0xFF3B3B2E) else Color(0xFFFFF9C4))
                                Priority.High -> if (isSelected) (if (isDark) HighPriorityDark else Color(0xFFD32F2F)) else (if (isDark) Color(0xFF3B2E2E) else Color(0xFFFBE9E7))
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
                                    text = priority.title,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)),
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
                        text = stringResource(R.string.schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeOptions.forEach { time ->
                            val isSelected = selectedTimes.contains(time)
                            val chipColor = when(time) {
                                TimeSlot.Morning -> if (isSelected) (if (isDark) MorningBlueDark else Color(0xFF1976D2)) else (if (isDark) Color(0xFF2E343B) else Color(0xFFE3F2FD))
                                TimeSlot.Afternoon -> if (isSelected) (if (isDark) AfternoonGreenDark else Color(0xFF388E3C)) else (if (isDark) Color(0xFF2E3B2E) else Color(0xFFF1F8E9))
                                TimeSlot.Evening -> if (isSelected) (if (isDark) EveningRedDark else Color(0xFFD32F2F)) else (if (isDark) Color(0xFF3B2E2E) else Color(0xFFFBE9E7))
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
                                    text = time.title,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)),
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
                            onConfirm(name, tablets, selectedTimes.toList(), selectedPriority)
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
                        Text(stringResource(R.string.add_to_list), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel), color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
