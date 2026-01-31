package com.example.simplemedicinechecklist

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplemedicinechecklist.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    Splash,
    AddMedicine,
    Checklist,
    Options,
    MonthlyStatus,
    About
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleMedicineChecklistTheme {
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val viewModel: MedicineViewModel = viewModel(
                    factory = MedicineViewModelFactory(database.medicineDao())
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedicineAppContainer(viewModel)
                }
            }
        }
    }
}

@Composable
fun MedicineAppContainer(viewModel: MedicineViewModel) {
    val medicines by viewModel.medicines.collectAsState()
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

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("medicine_prefs", Context.MODE_PRIVATE)
        while(true) {
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastResetDate = sharedPref.getString("last_reset_date", "")
            
            if (currentDateStr != lastResetDate) {
                viewModel.resetAllMedicines()
                viewModel.pruneOldRecords()
                sharedPref.edit { putString("last_reset_date", currentDateStr) }
            }
            viewModel.updateDate()
            delay(30000) 
        }
    }

    LaunchedEffect(Unit) {
        delay(2500) 
        currentScreen = if (medicines.isNotEmpty()) {
            Screen.Checklist
        } else {
            isFromOptions = false
            Screen.AddMedicine
        }
    }

    LaunchedEffect(medicines) {
        if (currentScreen != Screen.Splash) {
            if (medicines.isEmpty() && currentScreen == Screen.Checklist) {
                isFromOptions = false
                currentScreen = Screen.AddMedicine
            }
        }
    }

    BackHandler(enabled = currentScreen != Screen.Checklist && currentScreen != Screen.Splash) {
        if (medicines.isEmpty() && currentScreen == Screen.AddMedicine) {
            activity?.finish()
        } else {
            when (currentScreen) {
                Screen.Options -> currentScreen = Screen.Checklist
                Screen.AddMedicine -> {
                    if (isFromOptions) {
                        currentScreen = Screen.Options
                        isFromOptions = false
                    }
                }
                Screen.MonthlyStatus -> currentScreen = Screen.Options
                Screen.About -> currentScreen = Screen.Options
                else -> {}
            }
        }
    }

    when (currentScreen) {
        Screen.Splash -> SplashScreen()
        Screen.AddMedicine -> {
            AddMedicinePage(
                viewModel = viewModel,
                medicines = medicines,
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
                medicines = medicines,
                onOpenOptions = { currentScreen = Screen.Options }
            )
        }
        Screen.Options -> {
            OptionsPage(
                onNavigateToManage = { 
                    isFromOptions = true
                    currentScreen = Screen.AddMedicine 
                },
                onNavigateToMonthlyStatus = { currentScreen = Screen.MonthlyStatus },
                onNavigateToAbout = { currentScreen = Screen.About },
                onBack = { currentScreen = Screen.Checklist }
            )
        }
        Screen.MonthlyStatus -> {
            MonthlyStatusPage(
                viewModel = viewModel,
                medicines = medicines,
                onBack = { currentScreen = Screen.Options }
            )
        }
        Screen.About -> {
            AboutPage(
                onBack = { currentScreen = Screen.Options }
            )
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
                            painter = painterResource(id = R.drawable.app_icon_v2),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize().scale(1.5f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Daily Medicine Checklist",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = BluePrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simple daily medicine tracking",
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
fun AddMedicinePage(
    viewModel: MedicineViewModel,
    medicines: List<Medicine>,
    showBackIcon: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Medicines", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    if (showBackIcon) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onDone,
                        enabled = medicines.isNotEmpty()
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            color = if (medicines.isNotEmpty()) BluePrimary else Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Medicine") },
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
            if (medicines.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.MedicalServices,
                    message = "Your medicine list is empty.\nTap the button below to add your first medicine."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(medicines) { medicine ->
                        MedicineEditCard(
                            medicine = medicine,
                            onDelete = { medicineToDelete = medicine }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMedicineDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, tablets, times ->
                    viewModel.addMedicine(name, tablets, times)
                    showAddDialog = false
                }
            )
        }

        if (medicineToDelete != null) {
            AlertDialog(
                onDismissRequest = { medicineToDelete = null },
                title = { Text("Delete Medicine", color = TextHeader) },
                text = { Text("Are you sure you want to delete ${medicineToDelete?.name}?", color = TextPrimary) },
                confirmButton = {
                    Button(
                        onClick = {
                            medicineToDelete?.let { viewModel.deleteMedicine(it) }
                            medicineToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { medicineToDelete = null }) {
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
    viewModel: MedicineViewModel,
    medicines: List<Medicine>,
    onOpenOptions: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Breakfast", "Lunch", "Dinner")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTabTitle = tabs[selectedTabIndex]
    
    val currentDateStr by viewModel.currentDate.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val isSaved = todayRecords.any { it.timeSlot == currentTabTitle }

    val breakfastCompleted = todayRecords.any { it.timeSlot == "Breakfast" }
    val lunchCompleted = todayRecords.any { it.timeSlot == "Lunch" }
    val dinnerCompleted = todayRecords.any { it.timeSlot == "Dinner" }

    val currentTabMedicines = remember(medicines, todayRecords, selectedTabIndex) {
        if (isSaved) {
            val savedNames = todayRecords.filter { it.timeSlot == currentTabTitle }.map { it.medicineName }
            medicines.filter { savedNames.contains(it.name) }
        } else {
            medicines.filter { it.times.contains(currentTabTitle) }
        }
    }

    val allTaken = remember(currentTabMedicines, selectedTabIndex) {
        currentTabMedicines.isNotEmpty() && currentTabMedicines.all {
            when (currentTabTitle) {
                "Breakfast" -> it.isTakenBreakfast
                "Lunch" -> it.isTakenLunch
                "Dinner" -> it.isTakenDinner
                else -> false
            }
        }
    }

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
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
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
                                Toast.makeText(context, "Data updated for the new day!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Already up to date", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextHeader.copy(alpha = 0.7f),
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                    IconButton(onClick = onOpenOptions) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Options",
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
                        if (!allTaken) {
                            Toast.makeText(context, "Please check all medicines first", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveDailyRecords(currentTabMedicines, currentTabTitle)
                            Toast.makeText(context, "$currentTabTitle records saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaved && currentTabMedicines.isNotEmpty(),
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
                    Icon(if (isSaved) Icons.Default.DoneAll else Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isSaved) "$currentTabTitle Records Saved" else "Save $currentTabTitle Medicines",
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
                        text = "Daily Medicine Checklist for",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = TextHeader
                    )
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
                    text = "Breakfast",
                    isSelected = selectedTabIndex == 0,
                    isCompleted = breakfastCompleted,
                    onClick = { selectedTabIndex = 0 },
                    bgColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = "Lunch",
                    isSelected = selectedTabIndex == 1,
                    isCompleted = lunchCompleted,
                    onClick = { selectedTabIndex = 1 },
                    bgColor = Color(0xFFF1F8E9),
                    textColor = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                CustomTabButton(
                    text = "Dinner",
                    isSelected = selectedTabIndex == 2,
                    isCompleted = dinnerCompleted,
                    onClick = { selectedTabIndex = 2 },
                    bgColor = Color(0xFFFBE9E7),
                    textColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total number of medicines : ${currentTabMedicines.size}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = TextHeader
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentTabMedicines.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.DoneAll,
                        message = "No medicines scheduled for $currentTabTitle."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(currentTabMedicines) { medicine ->
                        val isTaken = if (isSaved) {
                            true 
                        } else {
                            when (currentTabTitle) {
                                "Breakfast" -> medicine.isTakenBreakfast
                                "Lunch" -> medicine.isTakenLunch
                                "Dinner" -> medicine.isTakenDinner
                                else -> false
                            }
                        }
                        MedicineChecklistCard(
                            medicine = medicine,
                            isTaken = isTaken,
                            onToggle = { if (!isSaved) viewModel.toggleMedicineTaken(medicine, currentTabTitle) }
                        )
                    }
                }
            }
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
            LargeTopAppBar(
                title = { Text("Options", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
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
                text = "Manage Medicines",
                icon = Icons.Default.AddCircle,
                color = BluePrimary,
                onClick = onNavigateToManage
            )
            OptionButton(
                text = "Monthly Status",
                icon = Icons.Default.CalendarMonth,
                color = BluePrimary,
                onClick = onNavigateToMonthlyStatus
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
                color = TextPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatusPage(viewModel: MedicineViewModel, medicines: List<Medicine>, onBack: () -> Unit) {
    var selectedMonthIndex by remember { mutableIntStateOf(0) } 
    val currentMonthRecords by viewModel.getRecordsForMonth(0).collectAsState(initial = emptyList())
    val previousMonthRecords by viewModel.getRecordsForMonth(-1).collectAsState(initial = emptyList())

    val records = if (selectedMonthIndex == 0) currentMonthRecords else previousMonthRecords
    
    val calendar = Calendar.getInstance()
    if (selectedMonthIndex == 1) calendar.add(Calendar.MONTH, -1)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Monthly Status Report", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
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
                        TableHeaderCell("Breakfast", Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell("Lunch", Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)
                        TableHeaderCell("Dinner", Modifier.weight(1f))
                    }

                    val dates = remember(selectedMonthIndex) { 
                        getDatesForMonth(if (selectedMonthIndex == 0) 0 else -1) 
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dates) { date ->
                            StatusRow(date, records, medicines)
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
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else BluePrimary,
        label = "contentColor"
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
fun StatusRow(date: Date, records: List<MedicineRecord>, medicines: List<Medicine>) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    val displayDate = formatReportDate(date)
    
    val rowRecords = records.filter { it.date == dateStr }
    
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(displayDate, modifier = Modifier.weight(1.2f).padding(vertical = 12.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary)
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Breakfast", date, rowRecords, medicines))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Lunch", date, rowRecords, medicines))
        VerticalDivider(color = Color.Gray.copy(alpha = 0.75f), thickness = 0.5.dp)
        StatusCell(Modifier.weight(1f).fillMaxHeight(), getStatus("Dinner", date, rowRecords, medicines))
    }
}

@Composable
fun StatusCell(modifier: Modifier, status: StatusType) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (status) {
            StatusType.Taken -> Icon(Icons.Default.CheckBox, contentDescription = "Taken", tint = Color(0xFF388E3C), modifier = Modifier.size(24.dp))
            StatusType.Missed -> Icon(Icons.Default.DisabledByDefault, contentDescription = "Missed", tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
            StatusType.None -> Text("—", color = Color.LightGray, fontWeight = FontWeight.Bold)
            StatusType.Future -> { /* Empty */ }
        }
    }
}

enum class StatusType { Taken, Missed, None, Future }

fun getStatus(slot: String, date: Date, records: List<MedicineRecord>, medicines: List<Medicine>): StatusType {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    if (date.after(today.time)) return StatusType.Future

    val isScheduled = medicines.any { it.times.contains(slot) }
    if (!isScheduled) return StatusType.None

    val record = records.find { it.timeSlot == slot }
    return if (record != null) StatusType.Taken else StatusType.Missed
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
fun AboutPage(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Simple Medicine Checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Designed to help you track your daily medication.", textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
fun MedicineEditCard(medicine: Medicine, onDelete: () -> Unit) {
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
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = BluePrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                val tabletText = if (medicine.numberOfTablets == "1") "tablet" else "tablets"
                Text(
                    text = "${medicine.numberOfTablets} $tabletText • ${medicine.times}",
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
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun MedicineChecklistCard(medicine: Medicine, isTaken: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isTaken) 
            Color(0xFFEEF6FB) 
        else 
            Color.White,
        label = "color"
    )

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
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isTaken) TextSecondary else TextPrimary
                    )
                )
                val tabletText = if (medicine.numberOfTablets == "1") "tablet" else "tablets"
                Text(
                    text = "${medicine.numberOfTablets} $tabletText",
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
            contentDescription = null,
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
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tablets by remember { mutableStateOf("") }
    val timeOptions = listOf("Breakfast", "Lunch", "Dinner")
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = BluePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "New Medicine",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeader
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medicine Name") },
                        placeholder = { Text("e.g. Paracetamol") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary
                        )
                    )
                    OutlinedTextField(
                        value = tablets,
                        onValueChange = { tablets = it },
                        label = { Text("Number of tablets") },
                        placeholder = { Text("e.g. 1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = BluePrimary
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
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
                                "Breakfast" -> if (isSelected) Color(0xFF1976D2) else Color(0xFFE3F2FD)
                                "Lunch" -> if (isSelected) Color(0xFF388E3C) else Color(0xFFF1F8E9)
                                "Dinner" -> if (isSelected) Color(0xFFD32F2F) else Color(0xFFFBE9E7)
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
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = time,
                                    color = if (isSelected) Color.White else chipColor.copy(alpha = 0.8f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm(name, tablets, selectedTimes.joinToString(", ")) },
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
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
