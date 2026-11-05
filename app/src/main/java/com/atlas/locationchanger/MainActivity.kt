package com.atlas.locationchanger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LocationChangerScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun LocationChangerScreen() {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(russianCities.first()) }
    var isActive by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("Сначала выбери город и настрой Mock Location") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        info = if (granted) "Разрешение на геолокацию получено" else "Нужно разрешить доступ к местоположению"
    }

    val filtered = russianCities.filter {
        it.name.contains(search, ignoreCase = true) || it.region.contains(search, ignoreCase = true)
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF245FE8),
        background = Color(0xFFF4F6FA),
        surface = Color.White
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Text("MLBB Location Changer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Подмена GPS-координат · Россия", color = Color(0xFF667085))
                Spacer(Modifier.height(16.dp))

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF14243B))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(if (isActive) "ПОДМЕНА ВКЛЮЧЕНА" else "ПОДМЕНА ВЫКЛЮЧЕНА", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(selected.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${selected.latitude}, ${selected.longitude}", color = Color(0xFFB8C7DA))
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (!fine) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                } else {
                                    val intent = Intent(context, LocationSpoofService::class.java).apply {
                                        putExtra(LocationSpoofService.EXTRA_LAT, selected.latitude)
                                        putExtra(LocationSpoofService.EXTRA_LON, selected.longitude)
                                        putExtra(LocationSpoofService.EXTRA_CITY, selected.name)
                                    }
                                    try {
                                        ContextCompat.startForegroundService(context, intent)
                                        isActive = true
                                        info = "Запрос на включение отправлен. Проверь статус в уведомлениях."
                                    } catch (e: Exception) {
                                        info = "Не удалось запустить: ${e.message}"
                                    }
                                }
                            }) { Text("Включить") }
                            OutlinedButton(onClick = {
                                context.stopService(Intent(context, LocationSpoofService::class.java))
                                isActive = false
                                info = "Подмена остановлена"
                            }) { Text("Остановить") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(info, color = Color(0xFF475467), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                        catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                ) { Text("Открыть настройки разработчика") }
                Text("В настройках выбери «Приложение для фиктивных местоположений» → MLBB Location Changer.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))

                Spacer(Modifier.height(16.dp))
                Text("Выбрать город", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Поиск города или региона") }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(filtered) { city ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(containerColor = if (city == selected) Color(0xFFE8EEFF) else Color.White),
                            onClick = { selected = city; info = "Выбрано: ${city.name}" }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(city.name, fontWeight = FontWeight.SemiBold)
                                    Text(city.region, color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(if (city == selected) "Выбрано" else "Выбрать", color = Color(0xFF245FE8))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("GPS-подмена не меняет IP, регион SIM или сервер игры. Некоторые приложения могут обнаруживать Mock Location.", color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
