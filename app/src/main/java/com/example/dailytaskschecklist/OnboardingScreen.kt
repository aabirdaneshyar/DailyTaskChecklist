package com.example.dailytaskschecklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dailytaskschecklist.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingData(
    val titleRes: Int,
    val descriptionRes: Int,
    val imageRes: Int? = null,
    val illustration: @Composable (ColumnScope.() -> Unit)? = null
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val dim = LocalAppDimensions.current
    val onboardingPages = listOf(
        OnboardingData(
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_desc_1,
            imageRes = R.drawable.icon_tasks
        ),
        OnboardingData(
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_desc_2,
            illustration = { AddTaskIllustration() }
        ),
        OnboardingData(
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_desc_3,
            illustration = { TimeSlotIllustration() }
        ),
        OnboardingData(
            titleRes = R.string.onboarding_title_4,
            descriptionRes = R.string.onboarding_desc_4,
            illustration = { ProgressIllustration() }
        )
    )

    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Skip Button
        AnimatedVisibility(
            visible = pagerState.currentPage < onboardingPages.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dim.paddingLarge + 24.dp, end = dim.paddingMedium)
                .zIndex(1f)
        ) {
            TextButton(
                onClick = { onFinished() }
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    color = if (isDark) BluePrimaryDark else BluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = dim.bodySize
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPage(data = onboardingPages[page])
            }

            // Bottom Navigation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = dim.paddingLarge + 8.dp)
                    .padding(bottom = dim.paddingLarge + 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(
                    modifier = Modifier.padding(bottom = dim.paddingLarge + 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(onboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) (if (dim.headerSize.value > 30f) 18.dp else 12.dp) else (if (dim.headerSize.value > 30f) 12.dp else 8.dp))
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Next / Get Started Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dim.buttonHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage == onboardingPages.size - 1) {
                            stringResource(R.string.get_started)
                        } else {
                            stringResource(R.string.next)
                        },
                        fontSize = (dim.bodySize.value + 2f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(data: OnboardingData) {
    val dim = LocalAppDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dim.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (dim.headerSize.value > 30f) 360.dp else 240.dp)
                .padding(bottom = dim.paddingLarge + 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (data.imageRes != null) {
                Surface(
                    modifier = Modifier.size(if (dim.headerSize.value > 30f) 260.dp else 180.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp
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
                            painter = painterResource(id = data.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else if (data.illustration != null) {
                data.illustration.invoke(this@Column)
            }
        }

        Text(
            text = stringResource(data.titleRes),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = (dim.headerSize.value + 4f).sp,
                lineHeight = (dim.headerSize.value + 16f).sp
            ),
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(data.descriptionRes),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = dim.bodySize,
                lineHeight = (dim.bodySize.value + 12f).sp
            ),
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = dim.paddingMedium)
        )
    }
}

@Composable
fun AddTaskIllustration() {
    val dim = LocalAppDimensions.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(if (dim.headerSize.value > 30f) 0.6f else 0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(dim.paddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, if (dim.headerSize.value > 30f) 36.dp else 24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)) 
                )
                Spacer(modifier = Modifier.width(dim.paddingSmall + 4.dp))
                Column {
                    Box(modifier = Modifier.width(if (dim.headerSize.value > 30f) 120.dp else 80.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(if (dim.headerSize.value > 30f) 60.dp else 40.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape))
                }
            }
        }
        Spacer(modifier = Modifier.height(dim.paddingMedium))
        Surface(
            modifier = Modifier.size(if (dim.headerSize.value > 30f) 84.dp else 56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(if (dim.headerSize.value > 30f) 20.dp else 12.dp)
            )
        }
    }
}

@Composable
fun TimeSlotIllustration() {
    val dim = LocalAppDimensions.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                val color = when(index) {
                    0 -> MaterialTheme.colorScheme.primary
                    1 -> Color(0xFF22C55E) 
                    else -> Color(0xFFEF4444) 
                }
                Surface(
                    modifier = Modifier.size(if (dim.headerSize.value > 30f) 100.dp else 64.dp, if (dim.headerSize.value > 30f) 64.dp else 40.dp),
                    shape = RoundedCornerShape(if (dim.headerSize.value > 30f) 32.dp else 20.dp),
                    color = if (index == 0) color else color.copy(alpha = 0.1f),
                    border = if (index != 0) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (index == 0) {
                            Box(modifier = Modifier.width(if (dim.headerSize.value > 30f) 50.dp else 30.dp).height(if (dim.headerSize.value > 30f) 6.dp else 4.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressIllustration() {
    val dim = LocalAppDimensions.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(if (dim.headerSize.value > 30f) 0.7f else 0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(dim.paddingSmall)) {
                repeat(4) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { colIndex ->
                            if (rowIndex == 0) {
                                Box(modifier = Modifier.size(if (dim.headerSize.value > 30f) 36.dp else 24.dp, 8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape))
                            } else {
                                if (colIndex > 0 && (rowIndex + colIndex) % 3 == 0) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(if (dim.headerSize.value > 30f) 28.dp else 20.dp))
                                } else {
                                    Box(modifier = Modifier.size(if (dim.headerSize.value > 30f) 24.dp else 16.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape))
                                }
                            }
                        }
                    }
                    if (rowIndex < 3) HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }
}
