package com.snappurge.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snappurge.app.audio.SoundEffectsHelper
import com.snappurge.app.cleaner.CleanupWorker
import com.snappurge.app.cleaner.ScreenshotCleaner
import com.snappurge.app.cleaner.ScreenshotItem
import com.snappurge.app.data.CleanerPreferences
import com.snappurge.app.data.CleanerSettings
import com.snappurge.app.hardware.FlashlightHelper
import com.snappurge.app.hardware.HapticHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    prefs: CleanerPreferences,
    settings: CleanerSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val soundHelper = remember { SoundEffectsHelper(context) }
    val flashlightHelper = remember { FlashlightHelper(context) }
    val hapticHelper = remember { HapticHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            soundHelper.release()
        }
    }

    val s = if (settings.language == "en") StringsEn else StringsZh

    var isScanning by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }
    var previewItems by remember { mutableStateOf<List<ScreenshotItem>?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var celebrationTrigger by remember { mutableIntStateOf(0) }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var hasStorageManagerPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
        )
    }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageManagerPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else true
                isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 通过 RTL 包裹使抽屉从屏幕右侧滑出，贴合右上角齿轮图标的视觉流向
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        SettingsDrawerContent(
                            context = context,
                            prefs = prefs,
                            settings = settings,
                            strings = s,
                            soundHelper = soundHelper,
                            flashlightHelper = flashlightHelper,
                            hapticHelper = hapticHelper,
                            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                            onClose = { scope.launch { drawerState.close() } }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = s.appTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = s.appSubtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = s.drawerTitle
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // 1. 权限提醒条目（仅未授权时展示）
                        if (!hasStorageManagerPermission) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = s.permissionTitle,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = s.permissionDesc,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                    try {
                                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(s.btnGrantPermission)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. 首页核心：累计清理成果卡片
                        item {
                            StatsCard(
                                settings = settings,
                                s = s,
                                onClick = {
                                    if (settings.hapticEnabled) hapticHelper.tap()
                                    celebrationTrigger++
                                }
                            )
                        }

                        // 3. 首页核心：扫描预览与立即清理操作按钮区
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = s.manualCleanTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = s.manualCleanSubtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 扫描预览按钮（触发吸入式光盘音效 + 快速战术双闪 + 机械吸入触感）
                                        OutlinedButton(
                                            onClick = {
                                                if (settings.soundEffect != "none") {
                                                    soundHelper.playDiscInsert()
                                                }
                                                if (settings.flashEffectEnabled) {
                                                    scope.launch { flashlightHelper.strobe(times = 2) }
                                                }
                                                if (settings.hapticEnabled) {
                                                    hapticHelper.preview()
                                                }
                                                scope.launch {
                                                    isScanning = true
                                                    val items = withContext(Dispatchers.IO) {
                                                        ScreenshotCleaner.scan(
                                                            context = context,
                                                            prefixes = settings.prefixes,
                                                            olderThanHours = 0 // 手动模式扫描全部匹配项
                                                        )
                                                    }
                                                    isScanning = false
                                                    previewItems = items
                                                    showPreviewDialog = true
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isScanning && !isCleaning
                                        ) {
                                            if (isScanning) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Search, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(s.btnScanPreview)
                                            }
                                        }

                                        // 立即清理按钮（触发碎纸机/引擎音效 + 战术5连爆闪 + 递增马达爆破震感）
                                        Button(
                                            onClick = {
                                                soundHelper.play(settings.soundEffect)
                                                if (settings.flashEffectEnabled) {
                                                    scope.launch { flashlightHelper.strobe(times = 5) }
                                                }
                                                if (settings.hapticEnabled) {
                                                    hapticHelper.clean()
                                                }
                                                scope.launch {
                                                    isCleaning = true
                                                    val result = withContext(Dispatchers.IO) {
                                                        val items = ScreenshotCleaner.scan(
                                                            context = context,
                                                            prefixes = settings.prefixes,
                                                            olderThanHours = 0 // 手动立即清理无视保留时长，直接全清
                                                        )
                                                        val res = ScreenshotCleaner.clean(context, items)
                                                        prefs.recordCleanup(res.deletedCount, res.freedBytes)
                                                        res
                                                    }
                                                    isCleaning = false
                                                    celebrationTrigger++
                                                    val mb = result.freedBytes / (1024.0 * 1024.0)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            s.snackbarCleanSuccess(result.deletedCount, "%.1f".format(mb))
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            enabled = !isScanning && !isCleaning
                                        ) {
                                            if (isCleaning) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            } else {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(s.btnCleanNow)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. 底部微型引导提示
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.hintSettingsDrawer,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                // 撒花 & 烟花庆祝动效层
                CelebrationEffect(
                    trigger = celebrationTrigger,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

    // 扫描结果预览对话框
    if (showPreviewDialog && previewItems != null) {
        val count = previewItems!!.size
        val totalBytes = previewItems!!.sumOf { it.sizeBytes }
        val mb = totalBytes / (1024.0 * 1024.0)
        val spaceDisplay = if (mb >= 1024) "${"%.2f".format(mb / 1024.0)} GB" else "${"%.1f".format(mb)} MB"

        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text(s.dialogScanTitle) },
            text = {
                Column {
                    Text(s.dialogScanMatched(count))
                    Text(s.dialogScanFreed(spaceDisplay))
                    if (count > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${s.dialogScanPrefixes}${settings.prefixes.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = s.dialogScanMode,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (count > 0) {
                    TextButton(
                        onClick = {
                            showPreviewDialog = false
                            soundHelper.play(settings.soundEffect)
                            if (settings.flashEffectEnabled) {
                                scope.launch { flashlightHelper.strobe(times = 5) }
                            }
                            if (settings.hapticEnabled) {
                                hapticHelper.clean()
                            }
                            scope.launch {
                                isCleaning = true
                                val res = withContext(Dispatchers.IO) {
                                    val r = ScreenshotCleaner.clean(context, previewItems!!)
                                    prefs.recordCleanup(r.deletedCount, r.freedBytes)
                                    r
                                }
                                isCleaning = false
                                celebrationTrigger++
                                val freedMb = res.freedBytes / (1024.0 * 1024.0)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        s.snackbarCleanSuccess(res.deletedCount, "%.1f".format(freedMb))
                                    )
                                }
                            }
                        }
                    ) {
                        Text(s.btnCleanThese)
                    }
                } else {
                    TextButton(onClick = { showPreviewDialog = false }) {
                        Text(s.btnKnow)
                    }
                }
            },
            dismissButton = {
                if (count > 0) {
                    TextButton(onClick = { showPreviewDialog = false }) {
                        Text(s.btnCancel)
                    }
                }
            }
        )
    }
}

/**
 * 侧拉窗配置内容面板
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDrawerContent(
    context: Context,
    prefs: CleanerPreferences,
    settings: CleanerSettings,
    strings: AppStrings,
    soundHelper: SoundEffectsHelper,
    flashlightHelper: FlashlightHelper,
    hapticHelper: HapticHelper,
    isIgnoringBatteryOptimizations: Boolean,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var newPrefixText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 侧边栏头部
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.drawerTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.closeSettings
                )
            }
        }

        HorizontalDivider()

        // 侧边栏滚动配置列表
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. 语言切换卡片 (中文 / English)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.languageSectionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == "zh",
                            onClick = { scope.launch { prefs.setLanguage("zh") } },
                            label = { Text(strings.langZh) }
                        )
                        FilterChip(
                            selected = settings.language == "en",
                            onClick = { scope.launch { prefs.setLanguage("en") } },
                            label = { Text(strings.langEn) }
                        )
                    }
                }
            }

            // 0.5 清理音效卡片 (碎纸机 / 引擎轰鸣 / 静音)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.soundSectionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.soundEffect == "shredder",
                            onClick = {
                                scope.launch { prefs.setSoundEffect("shredder") }
                                soundHelper.play("shredder")
                            },
                            label = { Text(strings.soundShredder) }
                        )
                        FilterChip(
                            selected = settings.soundEffect == "engine",
                            onClick = {
                                scope.launch { prefs.setSoundEffect("engine") }
                                soundHelper.play("engine")
                            },
                            label = { Text(strings.soundEngine) }
                        )
                        FilterChip(
                            selected = settings.soundEffect == "none",
                            onClick = {
                                scope.launch { prefs.setSoundEffect("none") }
                            },
                            label = { Text(strings.soundNone) }
                        )
                    }
                }
            }

            // 0.6 战术闪光灯爆闪开关卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.flashSectionTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.flashSectionDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.flashEffectEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setFlashEffectEnabled(checked)
                                    if (checked) {
                                        flashlightHelper.strobe(times = 2)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 0.7 战术触感马达震动开关卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.hapticSectionTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.hapticSectionDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setHapticEnabled(checked)
                                    if (checked) {
                                        hapticHelper.clean()
                                    } else {
                                        hapticHelper.tap()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 1. 自定义匹配前缀卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.prefixTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 前缀标签列表
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.prefixes.forEach { prefix ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(prefix, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.deletePrefixDesc,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                scope.launch { prefs.removePrefix(prefix) }
                                            }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 输入新前缀
                    OutlinedTextField(
                        value = newPrefixText,
                        onValueChange = { newPrefixText = it },
                        placeholder = { Text(strings.prefixPlaceholder, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (newPrefixText.isNotBlank()) {
                                scope.launch {
                                    prefs.addPrefix(newPrefixText.trim())
                                    newPrefixText = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = newPrefixText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnAddPrefix)
                    }
                }
            }

            // 2. 保留时间阈值卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.retentionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = strings.retentionSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val options = listOf(
                        24 to strings.retention1d,
                        48 to strings.retention2d,
                        72 to strings.retention3d,
                        168 to strings.retention7d,
                        360 to strings.retention15d,
                        720 to strings.retention30d
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { (hours, label) ->
                            val selected = settings.retentionHours == hours
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    scope.launch { prefs.setRetentionHours(hours) }
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // 3. 后台定时任务配置卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.autoCleanTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.autoCleanDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoCleanupEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    prefs.setAutoCleanupEnabled(enabled)
                                    if (enabled) {
                                        CleanupWorker.schedule(context, settings.requireCharging)
                                    } else {
                                        CleanupWorker.cancel(context)
                                    }
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.chargeOnlyTitle,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.chargeOnlyDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.requireCharging,
                            onCheckedChange = { required ->
                                scope.launch {
                                    prefs.setRequireCharging(required)
                                    if (settings.autoCleanupEnabled) {
                                        CleanupWorker.schedule(context, required)
                                    }
                                }
                            }
                        )
                    }

                    // 小米/HyperOS/华为等系统后台省电无限制建议
                    if (settings.autoCleanupEnabled && !isIgnoringBatteryOptimizations) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.batteryOptTitle,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.batteryOptDesc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Text(strings.btnGoSettings)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatsCard(
    settings: CleanerSettings,
    s: AppStrings,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = s.statsTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 1. 清理数字居中展示
            Text(
                text = "${settings.totalDeletedCount}",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 54.sp
            )

            // 2. “张已清理截图”放在数字下方
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = s.statsUnitCount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 在下方显示释放了多少存储空间（小字体 12sp）
            val mb = settings.totalFreedBytes / (1024.0 * 1024.0)
            val display = if (mb >= 1024) "${"%.2f".format(mb / 1024.0)} GB" else "${"%.1f".format(mb)} MB"
            Text(
                text = "${s.statsFreedSpace}$display",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )

            // 4. 清理时间放在最后面（同样小字体 12sp）
            if (settings.lastCleanTimestamp > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val timeStr = sdf.format(Date(settings.lastCleanTimestamp))
                Text(
                    text = "${s.statsLastClean}$timeStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}
