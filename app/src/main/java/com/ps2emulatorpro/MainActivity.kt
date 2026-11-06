package com.ps2emulatorpro

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

private val Night = Color(0xFF080B13)
private val Panel = Color(0xFF111827)
private val PanelAlt = Color(0xFF172235)
private val ElectricBlue = Color(0xFF36B7FF)
private val NeonPurple = Color(0xFF9A6BFF)
private val Muted = Color(0xFF98A7BE)
private val Good = Color(0xFF5DE0B5)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 19)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 19)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = ElectricBlue,
                secondary = NeonPurple,
                background = Night,
                surface = Panel,
                onPrimary = Night,
                onBackground = Color.White,
                onSurface = Color.White
            )) { EmulatorApp() }
        }
    }
}

private data class GameEntry(val uri: String, val name: String, val bytes: Long, val favorite: Boolean)
private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Default.Home),
    GAMES("Jogos", Icons.Default.SportsEsports),
    PERFORMANCE("Desempenho", Icons.Default.Speed),
    CONTROLS("Controles", Icons.Default.Gamepad),
    SETTINGS("Configurações", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmulatorApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ps2pro", Context.MODE_PRIVATE) }
    val games = remember { mutableStateListOf<GameEntry>().apply { addAll(readGames(prefs)) } }
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var query by rememberSaveable { mutableStateOf("") }
    var dialogText by remember { mutableStateOf<String?>(null) }
    var biosUri by remember { mutableStateOf(prefs.getString("bios_uri", null)) }

    val gamePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = displayName(context, uri) ?: "Imagem selecionada"
            if (!name.lowercase(Locale.ROOT).endsWith(".iso")) {
                dialogText = "Formato não reconhecido. Nesta versão somente arquivos .iso são listados; o núcleo de emulação ainda não está integrado."
            } else {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val size = fileSize(context, uri)
                if (games.none { it.uri == uri.toString() }) {
                    games.add(GameEntry(uri.toString(), name, size, false))
                    writeGames(prefs, games)
                }
                dialogText = "ISO adicionada à biblioteca. Não é possível iniciar jogos nesta versão porque ainda não há um núcleo PS2 integrado."
            }
        }
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            biosUri = uri.toString()
            prefs.edit().putString("bios_uri", biosUri).apply()
            dialogText = "Referência da BIOS salva. Esta versão não valida nem utiliza a BIOS: o núcleo PS2 ainda não foi integrado."
        }
    }

    Scaffold(
        containerColor = Night,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D1320), contentColor = Muted) {
                AppTab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item.name,
                        onClick = { tab = item.name },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(Night).padding(padding)) {
            when (AppTab.valueOf(tab)) {
                AppTab.HOME -> HomeScreen(onOpenGames = { tab = AppTab.GAMES.name }, onOpenPerformance = { tab = AppTab.PERFORMANCE.name })
                AppTab.GAMES -> GamesScreen(
                    games = games,
                    query = query,
                    onQuery = { query = it },
                    onAdd = { gamePicker.launch(arrayOf("application/octet-stream", "application/x-iso9660-image", "*/*")) },
                    onPlay = { dialogText = "Execução indisponível: o projeto ainda não inclui um núcleo real de emulação PS2. Nenhum jogo será simulado como se estivesse rodando." },
                    onFavorite = { game ->
                        val index = games.indexOfFirst { it.uri == game.uri }
                        if (index >= 0) {
                            games[index] = games[index].copy(favorite = !games[index].favorite)
                            writeGames(prefs, games)
                        }
                    },
                    onRemove = { game ->
                        games.removeAll { it.uri == game.uri }
                        writeGames(prefs, games)
                    }
                )
                AppTab.PERFORMANCE -> PerformanceScreen()
                AppTab.CONTROLS -> ControlsScreen()
                AppTab.SETTINGS -> SettingsScreen(
                    biosUri = biosUri,
                    onPickBios = { biosPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    onClearBios = {
                        biosUri = null
                        prefs.edit().remove("bios_uri").apply()
                    },
                    onInfo = { dialogText = it }
                )
            }
        }
    }
    dialogText?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogText = null },
            title = { Text("PS2 EMULATOR PRO", fontWeight = FontWeight.Bold) },
            text = { Text(message, color = Muted) },
            confirmButton = { TextButton(onClick = { dialogText = null }) { Text("ENTENDI") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun Header(eyebrow: String, title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(eyebrow.uppercase(), color = ElectricBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun HomeScreen(onOpenGames: () -> Unit, onOpenPerformance: () -> Unit) {
    val stats = rememberSystemStats()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Console portátil • Android", "PS2 EMULATOR PRO", "SEU PLAYSTATION 2 NA PALMA DA SUA MÃO.")
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PanelAlt)
            ) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF142D4A), Color(0xFF251846), PanelAlt))).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cable, null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ESTADO DO SISTEMA", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Núcleo não integrado", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("A interface está pronta para desenvolvimento; jogos não podem ser executados nesta versão.", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onOpenGames, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.SportsEsports, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ABRIR BIBLIOTECA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("RAM DISPONÍVEL", stats.ramAvailable, Icons.Default.Memory, Modifier.weight(1f))
                StatTile("BATERIA", stats.battery, Icons.Default.BatteryFull, Modifier.weight(1f))
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("TEMPERATURA", stats.temperature, Icons.Default.Thermostat, Modifier.weight(1f))
                StatTile("ARMAZENAMENTO LIVRE", stats.storageFree, Icons.Default.Storage, Modifier.weight(1f))
            }
            SectionTitle("Acesso rápido")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Biblioteca", Icons.Default.FolderOpen, onOpenGames, Modifier.weight(1f))
                QuickAction("Diagnóstico", Icons.Default.QueryStats, onOpenPerformance, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            InfoBanner("Compatibilidade", "Identificação automática, capas, execução, áudio e FPS dependem de um núcleo de emulação ainda não incluído.")
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GamesScreen(
    games: List<GameEntry>, query: String, onQuery: (String) -> Unit, onAdd: () -> Unit,
    onPlay: (GameEntry) -> Unit, onFavorite: (GameEntry) -> Unit, onRemove: (GameEntry) -> Unit
) {
    val filtered = games.filter { it.name.contains(query, ignoreCase = true) }
        .sortedWith(compareByDescending<GameEntry> { it.favorite }.thenBy { it.name.lowercase(Locale.ROOT) })
    Column(Modifier.fillMaxSize()) {
        Header("Sua coleção", "Biblioteca de jogos", "Arquivos ISO selecionados pelo armazenamento Android")
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.weight(1f), color = Panel, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Muted)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(value = query, onValueChange = onQuery, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), decorationBox = { inner ->
                        if (query.isEmpty()) Text("Pesquisar jogos", color = Muted, fontSize = 13.sp)
                        inner()
                    })
                }
            }
            Spacer(Modifier.width(9.dp))
            Button(onClick = onAdd, shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Add, "Adicionar ISO")
                Text("ISO", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoBanner("Núcleo necessário", "A biblioteca organiza arquivos, mas não consegue carregar ou executar jogos sem um núcleo PS2 real.", Modifier.padding(horizontal = 16.dp))
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(35.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DiscFull, null, tint = NeonPurple, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (games.isEmpty()) "Sua biblioteca está vazia" else "Nenhum jogo encontrado", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Adicione uma imagem ISO que você tenha direito de usar.", color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.uri }) { game ->
                    GameCard(game, onPlay = { onPlay(game) }, onFavorite = { onFavorite(game) }, onRemove = { onRemove(game) })
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onPlay: () -> Unit, onFavorite: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(NeonPurple, Color(0xFF174A7A)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DiscFull, null, tint = Color.White, modifier = Modifier.size(31.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(game.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Região não identificada • ${formatBytes(game.bytes)}", color = Muted, fontSize = 10.sp)
                Text("Origem: armazenamento SAF", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                Button(onClick = onPlay, shape = RoundedCornerShape(9.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp), colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                    Text("JOGAR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFavorite) { Icon(if (game.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favoritar", tint = if (game.favorite) NeonPurple else Muted) }
                IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, "Remover", tint = Muted) }
            }
        }
    }
}

@Composable
private fun PerformanceScreen() {
    val stats = rememberSystemStats()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Monitoramento do dispositivo", "Desempenho", "Somente informações fornecidas pelo Android")
            SectionTitle("Processador")
            InfoCard(listOf("SoC / hardware" to stats.cpu, "Núcleos lógicos" to stats.cores, "Uso de CPU" to "Indisponível via API pública"))
            SectionTitle("Gráficos")
            InfoCard(listOf("Vulkan reportado" to stats.vulkan, "OpenGL ES" to stats.openGl, "FPS do emulador" to "Indisponível — núcleo não integrado"))
            SectionTitle("Memória e armazenamento")
            InfoCard(listOf("RAM total" to stats.ramTotal, "RAM disponível" to stats.ramAvailable, "Memória do processo" to stats.appMemory, "Armazenamento livre" to stats.storageFree))
            SectionTitle("Bateria e sensores")
            InfoCard(listOf("Carga" to stats.battery, "Estado" to stats.charging, "Temperatura da bateria" to stats.temperature))
            Spacer(Modifier.height(12.dp))
            InfoBanner("Limites do Android", "Frequências detalhadas, temperatura interna da CPU/GPU e uso exato de CPU podem não ser acessíveis a aplicativos comuns. Nenhum valor estimado é inventado.", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ControlsScreen() {
    var pressed by remember { mutableStateOf("") }
    var opacity by remember { mutableFloatStateOf(0.65f) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Entrada", "Controles", "Prévia visual • ainda não conectada a um núcleo de jogo")
            InfoBanner("Estado", "Toques nesta tela apenas demonstram o feedback visual. Controles Bluetooth/USB e mapeamento para jogos ainda não estão integrados.", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DPad(pressed, { pressed = it })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ControllerButton("△", pressed, { pressed = it }, 48)
                    Row { ControllerButton("□", pressed, { pressed = it }, 48); Spacer(Modifier.width(8.dp)); ControllerButton("○", pressed, { pressed = it }, 48) }
                    ControllerButton("×", pressed, { pressed = it }, 48)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L2", "L1", "SELECT", "START", "R1", "R2").forEach { ControllerButton(it, pressed, { pressed = it }, if (it.length > 2) 54 else 42) }
            }
            Text(if (pressed.isBlank()) "Prévia de controle" else "Botão pressionado: $pressed (demonstração)", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
            SectionTitle("Transparência dos botões")
            Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.2f..1f, modifier = Modifier.padding(horizontal = 20.dp))
            Text("${(opacity * 100).roundToInt()}% • ajuste aplicado à prévia", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 22.dp))
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DPad(pressed: String, onPress: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ControllerButton("▲", pressed, onPress, 44)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControllerButton("◀", pressed, onPress, 44)
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(PanelAlt))
            Spacer(Modifier.width(4.dp))
            ControllerButton("▶", pressed, onPress, 44)
        }
        ControllerButton("▼", pressed, onPress, 44)
        Text("D-PAD", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun ControllerButton(label: String, pressed: String, onPress: (String) -> Unit, size: Int) {
    val active = pressed == label
    Box(Modifier.padding(3.dp).size(size.dp).clip(CircleShape).background(if (active) ElectricBlue else PanelAlt).clickable { onPress(if (active) "" else label) }, contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = if (label.length > 2) 8.sp else 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(biosUri: String?, onPickBios: () -> Unit, onClearBios: () -> Unit, onInfo: (String) -> Unit) {
    var vsync by rememberSaveable { mutableStateOf(true) }
    var widescreen by rememberSaveable { mutableStateOf(false) }
    var profile by rememberSaveable { mutableStateOf("Equilibrado") }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header("Personalização", "Configurações", "Preferências locais • recursos de emulação aguardam o núcleo")
            SectionTitle("Perfil de desempenho")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Econômico", "Equilibrado", "Desempenho", "Personalizado").forEach { value ->
                    Surface(color = if (profile == value) ElectricBlue else Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { profile = value }) {
                        Text(value, color = if (profile == value) Night else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    }
                }
            }
            Text("Perfil: $profile • ainda não altera a velocidade de emulação.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            SectionTitle("Gráficos (preferências de interface)")
            PreferenceSwitch("V-Sync", "Será aplicado quando houver um renderizador integrado.", vsync) { vsync = it }
            PreferenceSwitch("Proporção 16:9", "Depende do jogo e do suporte do núcleo.", widescreen) { widescreen = it }
            PreferenceItem("Resolução", "Nativa • opções de escala indisponíveis sem renderizador", Icons.Default.Tune) { onInfo("As opções de resolução 2x/3x/4x serão habilitadas somente quando um núcleo e renderizador compatíveis forem integrados.") }
            SectionTitle("BIOS do usuário")
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (biosUri == null) "Nenhuma BIOS selecionada" else "Referência de BIOS configurada", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("A BIOS não é fornecida pelo aplicativo. Selecione somente uma cópia obtida legalmente do seu próprio console. A versão atual não a valida nem a carrega.", color = Muted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onPickBios) { Text("SELECIONAR") }
                        if (biosUri != null) TextButton(onClick = onClearBios) { Text("REMOVER") }
                    }
                }
            }
            SectionTitle("Sistema")
            PreferenceItem("Diagnóstico", "Dados e limites informados pelo Android", Icons.Default.MonitorHeart) { onInfo("Consulte a aba Desempenho. Alguns sensores, como temperatura de CPU/GPU, podem não estar disponíveis para apps comuns.") }
            PreferenceItem("Compatibilidade", "Núcleo de emulação: não integrado", Icons.Default.Info) { onInfo("Este projeto não contém código de emulação PS2. Não há compatibilidade de jogos a declarar nesta versão.") }
            PreferenceItem("Sobre", "PS2 EMULATOR PRO • versão 0.1.0", Icons.Default.Copyright) { onInfo("Projeto Android inicial. Sem núcleo de emulação, BIOS ou jogos distribuídos.") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreferenceSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(description, color = Muted, fontSize = 10.sp) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PreferenceItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ElectricBlue)
        Column(Modifier.padding(start = 12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(description, color = Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 10.dp))
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 5.dp)) {
            rows.forEachIndexed { index, pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pair.first, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(pair.second, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                if (index != rows.lastIndex) androidx.compose.material3.HorizontalDivider(color = Color(0xFF233148))
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = ElectricBlue, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(title, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(7.dp))
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NeonPurple)
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
        }
    }
}

@Composable
private fun InfoBanner(title: String, message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF101D2D)), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
            Column(Modifier.padding(start = 9.dp)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

private data class SystemStats(
    val cpu: String, val cores: String, val ramTotal: String, val ramAvailable: String,
    val appMemory: String, val storageFree: String, val battery: String, val charging: String,
    val temperature: String, val vulkan: String, val openGl: String
)

@Composable
private fun rememberSystemStats(): SystemStats {
    val context = LocalContext.current
    return remember(context) { readSystemStats(context) }
}

@Suppress("DEPRECATION")
private fun readSystemStats(context: Context): SystemStats {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mem)
    val runtime = Runtime.getRuntime()
    val processUsed = runtime.totalMemory() - runtime.freeMemory()
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Indisponível"
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> "Carregando / conectado"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não carregando"
        else -> "Indisponível"
    }
    val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val temp = if (tempTenths > 0) String.format(Locale.getDefault(), "%.1f °C (bateria)", tempTenths / 10f) else "Indisponível"
    val stat = StatFs(Environment.getDataDirectory().path)
    val storageFree = formatBytes(stat.availableBytes)
    val cpu = listOfNotNull(Build.SOC_MODEL.takeIf { Build.VERSION.SDK_INT >= 31 && it.isNotBlank() }, Build.HARDWARE.takeIf { it.isNotBlank() }).firstOrNull() ?: "Indisponível"
    val features = context.packageManager.systemAvailableFeatures
    val hasVulkan = features.any { it.name == "android.hardware.vulkan.level" || it.name == "android.hardware.vulkan.version" }
    val gles = context.packageManager.systemAvailableFeatures.firstOrNull { it.name == "android.hardware.opengles.aep" }
    val glVersion = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_CONFIGURATIONS).let { info ->
        val req = info.reqFeatures?.firstOrNull { it.name == "android.hardware.opengles.aep" }
        if (req != null) "AEP" else "Recurso consultado pelo sistema"
    }
    return SystemStats(
        cpu = cpu,
        cores = Runtime.getRuntime().availableProcessors().toString(),
        ramTotal = formatBytes(mem.totalMem),
        ramAvailable = formatBytes(mem.availMem),
        appMemory = formatBytes(processUsed),
        storageFree = storageFree,
        battery = pct,
        charging = charging,
        temperature = temp,
        vulkan = if (hasVulkan) "Recurso reportado" else "Não reportado",
        openGl = if (gles != null) "OpenGL ES • AEP" else glVersion
    )
}

private fun readGames(prefs: android.content.SharedPreferences): List<GameEntry> = runCatching {
    val array = JSONArray(prefs.getString("games", "[]"))
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        GameEntry(obj.getString("uri"), obj.getString("name"), obj.optLong("bytes", -1), obj.optBoolean("favorite", false))
    }
}.getOrDefault(emptyList())

private fun writeGames(prefs: android.content.SharedPreferences, games: List<GameEntry>) {
    val array = JSONArray()
    games.forEach { game ->
        array.put(JSONObject().put("uri", game.uri).put("name", game.name).put("bytes", game.bytes).put("favorite", game.favorite))
    }
    prefs.edit().putString("games", array.toString()).apply()
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

private fun fileSize(context: Context, uri: Uri): Long = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
    } ?: -1L
}.getOrDefault(-1L)

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Indisponível"
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1) String.format(Locale.getDefault(), "%.1f GB", gb) else String.format(Locale.getDefault(), "%.0f MB", bytes / 1024.0 / 1024.0)
}
