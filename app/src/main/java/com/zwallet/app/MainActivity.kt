package com.zwallet.app

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zwallet.app.data.*
import com.zwallet.app.ui.CardsScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0B0D10)
private val Surface = Color(0xFF171A20)
private val Surface2 = Color(0xFF222630)
private val Accent = Color(0xFFE5484D)
private val Muted = Color(0xFF9299A6)
private val Green = Color(0xFF35C98A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = WalletDatabase.get(this).walletDao()
        val prefs = getSharedPreferences("zwallet_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("nickname", null)?.takeIf { it.isNotBlank() }
        setContent {
            val vm: WalletViewModel = viewModel(factory = WalletViewModel.factory(dao))
            var showWelcome by remember { mutableStateOf(true) }
            ZWalletTheme {
                Crossfade(targetState = showWelcome, animationSpec = tween(500), label = "root") { welcome ->
                    if (welcome) {
                        WelcomeScreen(
                            savedName = savedName,
                            onNameSaved = { prefs.edit().putString("nickname", it).apply() },
                            onFinished = { showWelcome = false }
                        )
                    } else {
                        WalletScreen(vm)
                    }
                }
            }
        }
    }
}

class WalletViewModel(private val dao: WalletDao) : ViewModel() {
    val cards = dao.observeCards().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = dao.observeTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCard(name: String, bank: String, balance: Long) = viewModelScope.launch {
        if (name.isNotBlank() && balance >= 0) dao.insertCard(WalletCard(name = name.trim(), bank = bank, balance = balance))
    }

    fun addTransaction(card: WalletCard, type: String, amount: Long, note: String) = viewModelScope.launch {
        if (amount <= 0) return@launch
        if (type == "EXPENSE" && amount > card.balance) return@launch
        dao.insertTransaction(WalletTransaction(cardId = card.id, type = type, amount = amount, note = note.trim().ifBlank { if (type == "INCOME") "Pemasukan" else "Pengeluaran" }))
        dao.changeBalance(card.id, if (type == "INCOME") amount else -amount)
    }

    fun deleteCard(card: WalletCard) = viewModelScope.launch { dao.deleteCard(card) }

    companion object {
        fun factory(dao: WalletDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WalletViewModel(dao) as T
        }
    }
}

@Composable
private fun ZWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg, surface = Surface, primary = Accent,
            secondary = Green, onBackground = Color.White, onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun WelcomeScreen(savedName: String?, onNameSaved: (String) -> Unit, onFinished: () -> Unit) {
    var name by remember { mutableStateOf(savedName) }
    var input by remember { mutableStateOf("") }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }
    val inputAlpha = remember { Animatable(0f) }
    val greetAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        if (savedName == null) inputAlpha.animateTo(1f, tween(500))
    }
    LaunchedEffect(name) {
        if (name != null) {
            greetAlpha.snapTo(0f)
            greetAlpha.animateTo(1f, tween(800))
            delay(1500)
            onFinished()
        }
    }

    val submit: () -> Unit = {
        val n = input.trim()
        if (n.isNotEmpty()) { onNameSaved(n); name = n }
    }

    Box(
        Modifier.fillMaxSize().background(Bg)
            .background(Brush.radialGradient(listOf(Color(0x33E5B94A), Color.Transparent))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.zwallet_logo),
                contentDescription = "ZWallet",
                modifier = Modifier.size(if (name == null) 170.dp else 210.dp).graphicsLayer {
                    alpha = logoAlpha.value; scaleX = logoScale.value; scaleY = logoScale.value
                }
            )
            Spacer(Modifier.height(28.dp))
            if (name == null) {
                Column(
                    Modifier.graphicsLayer { alpha = inputAlpha.value; translationY = (1f - inputAlpha.value) * 30f },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Selamat datang di ZWallet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Siapa nama panggilanmu?", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = input, onValueChange = { if (it.length <= 20) input = it },
                        label = { Text("Nama panggilan") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { submit() }, enabled = input.isNotBlank() && inputAlpha.value > 0.9f) { Text("Lanjut") }
                }
            } else {
                Column(
                    Modifier.graphicsLayer { alpha = greetAlpha.value; translationY = (1f - greetAlpha.value) * 40f },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (savedName != null) "Welcome Back" else "Welcome",
                        color = Color(0xFFE5B94A), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(name ?: "", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WalletScreen(vm: WalletViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    var showAddCard by remember { mutableStateOf(false) }
    var showTransaction by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<WalletCard?>(null) }
    var tab by remember { mutableStateOf(0) }
    val total = cards.sumOf { it.balance }
    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            if (tab != 1) FloatingActionButton(
                onClick = { if (cards.isNotEmpty()) { selectedCard = cards.first(); showTransaction = true } else showAddCard = true },
                containerColor = Accent, contentColor = Color.White
            ) { Icon(Icons.Rounded.Add, contentDescription = "Tambah transaksi") }
        },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Rounded.Wallet, null) }, label = { Text("Wallet") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Rounded.CreditCard, null) }, label = { Text("Kartu") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Rounded.ArrowUpward, null) }, label = { Text("Aktivitas") })
            }
        }
    ) { padding ->
        if (tab == 1) CardsScreen(Modifier.fillMaxSize().padding(padding)) else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ZWALLET", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text(when(tab) { 1 -> "Kartu saya"; 2 -> "Aktivitas"; else -> "Ringkasan wallet" }, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("OFFLINE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Surface2, RoundedCornerShape(30.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
            if (tab == 0) {
                item {
                    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(24.dp)).padding(22.dp)) {
                        Text("TOTAL SALDO", color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(rupiah(total), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStat("Pemasukan", income, Green, Modifier.weight(1f))
                            MiniStat("Pengeluaran", expense, Accent, Modifier.weight(1f))
                        }
                    }
                }
            }
            if (tab != 2) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tab == 1) "Semua kartu" else "Kartu tersimpan", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAddCard = true }) { Icon(Icons.Rounded.Add, null, Modifier.size(16.dp)); Text("Tambah") }
                    }
                }
                if (cards.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CreditCard, null, tint = Muted, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada kartu", fontWeight = FontWeight.SemiBold)
                            Text("Tambahkan kartu dan masukkan saldo saat ini.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { showAddCard = true }) { Text("Tambah kartu pertama") }
                        }
                    }
                }
                items(cards, key = { it.id }) { card ->
                    CardItem(card, onDelete = { vm.deleteCard(card) }, onIncome = { selectedCard = card; showTransaction = true }, onExpense = { selectedCard = card; showTransaction = true })
                }
            }
            if (tab == 0 || tab == 2) {
                item { Text("Transaksi terbaru", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (transactions.isEmpty()) item { Text("Belum ada transaksi.", color = Muted, fontSize = 13.sp) }
                items(transactions.take(30), key = { it.id }) { tx ->
                    val cardName = cards.firstOrNull { it.id == tx.cardId }?.name ?: "Kartu dihapus"
                    TransactionRow(tx, cardName)
                }
            }
            item {
                Text(
                    "Credit: JEPIN666X",
                    color = Muted, fontSize = 11.sp, letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showAddCard) AddCardDialog(onDismiss = { showAddCard = false }, onSave = { name, bank, balance -> vm.addCard(name, bank, balance); showAddCard = false })
    if (showTransaction) TransactionDialog(
        cards = cards, initialCard = selectedCard,
        onDismiss = { showTransaction = false },
        onSave = { card, type, amount, note -> vm.addTransaction(card, type, amount, note); showTransaction = false }
    )
}

@Composable
private fun MiniStat(label: String, amount: Long, color: Color, modifier: Modifier) {
    Column(modifier.background(Surface2, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(rupiah(amount), color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardItem(card: WalletCard, onDelete: () -> Unit, onIncome: () -> Unit, onExpense: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Accent.copy(alpha = .16f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CreditCard, null, tint = Accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(card.bank, color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = Muted) }
        }
        Spacer(Modifier.height(12.dp))
        Text("SALDO SAAT INI", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(rupiah(card.balance), fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onIncome, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.ArrowDownward, null, tint = Green, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Pemasukan", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onExpense, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.ArrowUpward, null, tint = Accent, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Pengeluaran", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction, cardName: String) {
    val isIncome = tx.type == "INCOME"
    Row(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(15.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(Surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, tint = if (isIncome) Green else Accent)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.note, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("$cardName • ${SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(tx.createdAt))}", color = Muted, fontSize = 11.sp)
        }
        Text((if (isIncome) "+" else "-") + rupiah(tx.amount), color = if (isIncome) Green else Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    val presets = listOf("BCA", "BRI", "BNI", "Mandiri", "Jenius", "DANA", "GoPay", "OVO", "ShopeePay", "Lainnya")
    var bank by remember { mutableStateOf(presets.first()) }
    var name by remember { mutableStateOf("BCA") }
    var balance by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah kartu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pilih bank / dompet digital", color = Muted, fontSize = 12.sp)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(bank); Spacer(Modifier.weight(1f)); Text("⌄") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        presets.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { bank = item; if (item != "Lainnya") name = item; expanded = false }) }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kartu / rekening") }, singleLine = true)
                OutlinedTextField(value = balance, onValueChange = { balance = it.filter(Char::isDigit) }, label = { Text("Saldo sekarang (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, bank, balance.toLongOrNull() ?: 0L) }, enabled = name.isNotBlank() && (balance.toLongOrNull() ?: -1L) >= 0) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun TransactionDialog(
    cards: List<WalletCard>, initialCard: WalletCard?,
    onDismiss: () -> Unit, onSave: (WalletCard, String, Long, String) -> Unit
) {
    var selectedId by remember { mutableLongStateOf(initialCard?.id ?: cards.firstOrNull()?.id ?: -1L) }
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val selected = cards.firstOrNull { it.id == selectedId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Pemasukan") })
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Pengeluaran") })
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.name ?: "Pilih kartu"); Spacer(Modifier.weight(1f)); Text("⌄") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        cards.forEach { c -> DropdownMenuItem(text = { Text("${c.name} • ${rupiah(c.balance)}") }, onClick = { selectedId = c.id; expanded = false }) }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Keterangan") }, singleLine = true)
                if (type == "EXPENSE" && selected != null && (amount.toLongOrNull() ?: 0L) > selected.balance) {
                    Text("Saldo kartu tidak mencukupi.", color = Accent, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected != null) onSave(selected, type, amount.toLongOrNull() ?: 0L, note) },
                enabled = selected != null && (amount.toLongOrNull() ?: 0L) > 0 && !(type == "EXPENSE" && (amount.toLongOrNull() ?: 0L) > selected.balance)
            ) { Text("Simpan transaksi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun rupiah(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")
