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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplemedicinechecklist.ui.theme.*
import kotlinx.coroutines.delay
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
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    // Reactive Date & Reset Logic
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("medicine_prefs", Context.MODE_PRIVATE)
        while(true) {
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastResetDate = sharedPref.getString("last_reset_date", "")
            
            if (currentDateStr != lastResetDate) {
                viewModel.resetAllMedicines()
                viewModel.pruneOldRecords() // Keep only current and previous month
                sharedPref.edit { putString("last_reset_date", currentDateStr) }
            }
            viewModel.updateDate()
            delay(30000) 
        }
    }

    // Initial navigation logic
    LaunchedEffect(Unit) {
        delay(2500) 
        currentScreen = if (medicines.isNotEmpty()) {
            Screen.Checklist
        } else {
            Screen.AddMedicine
        }
    }

    // Reactive navigation for empty state
    LaunchedEffect(medicines) {
        if (currentScreen != Screen.Splash) {
            if (medicines.isEmpty() && currentScreen == Screen.Checklist) {
                currentScreen = Screen.AddMedicine
            }
        }
    }

    // Navigation back button handling
    BackHandler(enabled = currentScreen != Screen.Checklist && currentScreen != Screen.Splash) {
        if (medicines.isEmpty() && currentScreen == Screen.AddMedicine) {
            activity?.finish()
        } else {
            when (currentScreen) {
                Screen.Options -> currentScreen = Screen.Checklist
                Screen.AddMedicine -> currentScreen = Screen.Options
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
                onBack = { 
                    if (medicines.isEmpty()) activity?.finish() else currentScreen = Screen.Options 
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
                onNavigateToManage = { currentScreen = Screen.AddMedicine },
                onNavigateToMonthlyStatus = { currentScreen = Screen.MonthlyStatus },
                onNavigateToAbout = { currentScreen = Screen.About },
                onBack = { currentScreen = Screen.Checklist }
            )
        }
        Screen.MonthlyStatus -> {
            MonthlyStatusPage(
                viewModel = viewModel,
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
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize().padding(24.dp)
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simple daily medicine tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BluePrimary.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Manage Medicines", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
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
    val currentTabTitle = tabs[selectedTabIndex]
    
    val currentDateStr by viewModel.currentDate.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val isSaved = todayRecords.any { it.timeSlot == currentTabTitle }

    // Completion status for all tabs
    val breakfastCompleted = todayRecords.any { it.timeSlot == "Breakfast" }
    val lunchCompleted = todayRecords.any { it.timeSlot == "Lunch" }
    val dinnerCompleted = todayRecords.any { it.timeSlot == "Dinner" }

    // Current tab medicines logic
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
                    IconButton(onClick = { /* Refresh logic removed */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextHeader.copy(alpha = 0.7f)
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
                        containerColor = if (isSaved) Color.Gray else if (!allTaken) BluePrimary.copy(alpha = 0.5f) else BluePrimary,
                        contentColor = Color.White
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
                Text(
                    text = "Daily Checklist for",
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

            // New Custom Tab Row matching Tab buttons.png
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Breakfast Tab
                CustomTabButton(
                    text = "Breakfast",
                    isSelected = selectedTabIndex == 0,
                    isCompleted = breakfastCompleted,
                    onClick = { selectedTabIndex = 0 },
                    bgColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                // Lunch Tab
                CustomTabButton(
                    text = "Lunch",
                    isSelected = selectedTabIndex == 1,
                    isCompleted = lunchCompleted,
                    onClick = { selectedTabIndex = 1 },
                    bgColor = Color(0xFFF1F8E9),
                    textColor = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                // Dinner Tab
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
        // Red/Green Indicator outside the button
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
fun MonthlyStatusPage(viewModel: MedicineViewModel, onBack: () -> Unit) {
    val currentMonthRecords by viewModel.getRecordsForMonth(0).collectAsState(initial = emptyList())
    val previousMonthRecords by viewModel.getRecordsForMonth(-1).collectAsState(initial = emptyList())

    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    val previousMonthName = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Monthly Status", fontWeight = FontWeight.Bold, color = TextHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHeader)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                StatusMonthSection(title = currentMonthName, records = currentMonthRecords)
            }
            item {
                StatusMonthSection(title = previousMonthName, records = previousMonthRecords)
            }
        }
    }
}

@Composable
fun StatusMonthSection(title: String, records: List<MedicineRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (records.isEmpty()) {
                Text("No data available for this month.", color = TextSecondary)
            } else {
                val groupedByTime = records.groupBy { it.timeSlot }
                listOf("Breakfast", "Lunch", "Dinner").forEach { slot ->
                    val slotRecords = groupedByTime[slot] ?: emptyList()
                    val takenCount = slotRecords.count { it.wasTaken }
                    val totalCount = slotRecords.size
                    
                    if (totalCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = slot, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                text = "$takenCount / $totalCount taken",
                                color = if (takenCount == totalCount) Color(0xFF388E3C) else BluePrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { takenCount.toFloat() / totalCount.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (takenCount == totalCount) Color(0xFF388E3C) else BluePrimary,
                            trackColor = Color(0xFFE3F2FD)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
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
            Text("Designed to help you track your daily medication.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
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
                Text(
                    text = "${medicine.numberOfTablets} tablets • ${medicine.times}",
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
                Text(
                    text = "${medicine.numberOfTablets} tablets",
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tablets by remember { mutableStateOf("") }
    val timeOptions = listOf("Breakfast", "Lunch", "Dinner")
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "New Medicine", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextHeader
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    placeholder = { Text("e.g. Paracetamol") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.MedicalServices, null) }
                )
                OutlinedTextField(
                    value = tablets,
                    onValueChange = { tablets = it },
                    label = { Text("Number of tablets") },
                    placeholder = { Text("e.g. 1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Column {
                    Text(
                        "Schedule", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timeOptions.forEach { time ->
                            FilterChip(
                                selected = selectedTimes.contains(time),
                                onClick = {
                                    selectedTimes = if (selectedTimes.contains(time)) {
                                        selectedTimes - time
                                    } else {
                                        selectedTimes + time
                                    }
                                },
                                label = { Text(time) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && tablets.isNotBlank() && selectedTimes.isNotEmpty()) {
                        onConfirm(name, tablets, selectedTimes.joinToString(", "))
                    }
                },
                enabled = name.isNotBlank() && tablets.isNotBlank() && selectedTimes.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to List")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
