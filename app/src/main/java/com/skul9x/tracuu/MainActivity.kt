package com.skul9x.tracuu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skul9x.tracuu.data.DataProcessor
import com.skul9x.tracuu.data.GroupedResult
import com.skul9x.tracuu.data.OldUnitInfo
import com.skul9x.tracuu.ui.theme.*
import com.skul9x.tracuu.utils.DebugLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TraCuuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<GroupedResult>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        DebugLogger.log("MainActivity", "App started. Loading initial data...")
        // Kiểm tra xem file có tồn tại không bằng cách load thử
        val initialData = DataProcessor.loadAndSearch(context, "")
        if (initialData.isEmpty() && DebugLogger.logs.any { it.contains("CRITICAL") }) {
             Toast.makeText(context, "CẢNH BÁO: Thiếu file dữ liệu!", Toast.LENGTH_LONG).show()
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            if (query.isNotEmpty()) {
                isLoading = true
                results = DataProcessor.loadAndSearch(context, query)
                isLoading = false
            } else {
                results = emptyList()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* menu or empty */ }, enabled = false) { }
                Text(
                    text = "Tra Cứu Sáp Nhập 2025",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    ),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { showDebugDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Debug Logs",
                        tint = PrimaryBlue
                    )
                }
            }
            
            Text(
                text = "Hệ thống tra cứu thông tin sáp nhập Phường/Xã",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Search Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Nhập tên Phường/Xã mới",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            performSearch(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ví dụ: Phường 1, Xã A...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        leadingIcon = {
                             Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryBlue,
                                    strokeWidth = 2.dp
                                )
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    results = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                }
            }

            // Status Label
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                val statusText = when {
                    searchQuery.isEmpty() -> "Sẵn sàng tra cứu"
                    isLoading -> "Đang tìm kiếm..."
                    results.isEmpty() -> "Không tìm thấy kết quả nào"
                    else -> "Tìm thấy ${results.size} kết quả"
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (results.isEmpty() && searchQuery.isNotEmpty() && !isLoading) Color.Red else TextSecondary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Results List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results) { item ->
                    ResultCard(item)
                }
            }
        }

        if (showDebugDialog) {
            DebugDialog(onDismiss = { showDebugDialog = false })
        }
    }
}

@Composable
fun ResultCard(item: GroupedResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min) // Equal height columns
        ) {
            // New Unit Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF0F9FF)) // Light blue tint
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Badge(text = "ĐƠN VỊ MỚI", color = HighlightNew)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Xã ${item.newWard}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = "Tỉnh ${item.newProvince}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(BorderColor)
            )

            // Old Unit Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Badge(text = "ĐƠN VỊ CŨ", color = HighlightOld)
                Spacer(modifier = Modifier.height(8.dp))
                
                item.oldUnits.forEach { unit ->
                    MapLink(unit)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Huyện ${item.oldDistrict}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Tỉnh ${item.oldProvince}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun MapLink(unit: OldUnitInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clickable {
                try {
                    val encodedQuery = Uri.encode(unit.query)
                    val uri = Uri.parse("http://maps.google.com/maps?q=$encodedQuery")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    DebugLogger.error("UI", "Không mở được bản đồ", e)
                    Toast.makeText(context, "Không tìm thấy ứng dụng bản đồ", Toast.LENGTH_SHORT).show()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📍", 
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = unit.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DebugDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val logs = DebugLogger.logs

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Debug Console",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("ERROR") || log.contains("CRITICAL")) Color(0xFFFF6B6B) else Color(0xFF4ECDC4),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { DebugLogger.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                    
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("App Logs", DebugLogger.getAllLogsAsString())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Logs")
                    }
                }
            }
        }
    }
}