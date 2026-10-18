@file:OptIn(ExperimentalMaterial3Api::class)

package com.apparel.stok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apparel.stok.data.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) { StockApp() }
        }
    }
}

private val SIZES = listOf("XS", "S", "M", "L", "XL", "XXL", "STD")

@Composable
fun StockApp(vm: StockViewModel = viewModel()) {
    val products by vm.products.collectAsStateWithLifecycle()
    val all by vm.allProducts.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val onlyLow by vm.onlyLow.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stok Takip") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Ürün ekle") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SummaryRow(all)
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text("Ara: ad, SKU, renk, beden…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = onlyLow,
                    onClick = { vm.onlyLow.value = !onlyLow },
                    label = { Text("Sadece kritik stok") },
                )
            }
            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (all.isEmpty()) "Henüz ürün yok.\n\"Ürün ekle\" ile başlayın."
                        else "Eşleşen ürün yok.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(products, key = { it.id }) { p ->
                        ProductCard(
                            p = p,
                            onClick = { selectedId = p.id },
                            onMinus = { vm.changeStock(p, -1, "Hızlı çıkış") },
                            onPlus = { vm.changeStock(p, 1, "Hızlı giriş") },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        ProductDialog(null, onDismiss = { showAdd = false }) { vm.save(it); showAdd = false }
    }
    editing?.let { p ->
        ProductDialog(p, onDismiss = { editing = null }) { vm.save(it); editing = null }
    }
    selectedId?.let { id ->
        all.find { it.id == id }?.let { p -> StockDialog(
            p = p,
            vm = vm,
            onDismiss = { selectedId = null },
            onEdit = { selectedId = null; editing = p },
            onDelete = { vm.delete(p); selectedId = null },
        ) }
    }
}

@Composable
private fun SummaryRow(all: List<Product>) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Stat("Çeşit", all.size.toString(), Modifier.weight(1f))
        Stat("Toplam adet", all.sumOf { it.quantity }.toString(), Modifier.weight(1f))
        Stat("Kritik", all.count { it.isLow }.toString(), Modifier.weight(1f), warn = all.any { it.isLow })
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, warn: Boolean = false) {
    Card(
        modifier = modifier,
        colors = if (warn) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ProductCard(p: Product, onClick: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (p.isLow) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOf(p.sku, p.size, p.color).filter { it.isNotBlank() }.joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall)
                if (p.isLow) Text(
                    "Kritik stok (min ${p.minQuantity})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FilledTonalIconButton(onClick = onMinus, enabled = p.quantity > 0) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                p.quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp),
            )
            FilledTonalIconButton(onClick = onPlus) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ProductDialog(initial: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var sku by rememberSaveable { mutableStateOf(initial?.sku ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "") }
    var size by rememberSaveable { mutableStateOf(initial?.size ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: "") }
    var qty by rememberSaveable { mutableStateOf(initial?.quantity?.toString() ?: "0") }
    var minQty by rememberSaveable { mutableStateOf(initial?.minQuantity?.toString() ?: "5") }
    val isNew = initial == null
    val valid = name.isNotBlank() && (qty.toIntOrNull() ?: -1) >= 0 && (minQty.toIntOrNull() ?: -1) >= 0
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Yeni ürün" else "Ürünü düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Ürün adı *") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU / Model kodu") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Kategori (tişört, pantolon…)") }, singleLine = true)
                OutlinedTextField(size, { size = it }, label = { Text("Beden") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SIZES.forEach { s -> SuggestionChip(onClick = { size = s }, label = { Text(s) }) }
                }
                OutlinedTextField(color, { color = it }, label = { Text("Renk") }, singleLine = true)
                if (isNew) OutlinedTextField(
                    qty, { qty = it.filter(Char::isDigit) }, label = { Text("Açılış stoğu") },
                    singleLine = true, keyboardOptions = number,
                )
                OutlinedTextField(
                    minQty, { minQty = it.filter(Char::isDigit) }, label = { Text("Kritik stok seviyesi") },
                    singleLine = true, keyboardOptions = number,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val base = initial ?: Product(name = "")
                onSave(
                    base.copy(
                        name = name.trim(), sku = sku.trim(), category = category.trim(),
                        size = size.trim(), color = color.trim(),
                        quantity = if (isNew) qty.toInt() else base.quantity,
                        minQuantity = minQty.toInt(),
                    )
                )
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun StockDialog(
    p: Product,
    vm: StockViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var isIn by rememberSaveable { mutableStateOf(true) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val history by remember(p.id) { vm.movements(p.id) }.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr")) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Silinsin mi?") },
            text = { Text("\"${p.name}\" ve tüm hareket geçmişi silinecek.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(p.name)
                Text(
                    listOf(p.sku, p.size, p.color).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mevcut stok: ${p.quantity}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isIn, onClick = { isIn = true }, label = { Text("Giriş (+)") })
                    FilterChip(selected = !isIn, onClick = { isIn = false }, label = { Text("Çıkış (−)") })
                }
                OutlinedTextField(
                    amount, { amount = it.filter(Char::isDigit); error = null },
                    label = { Text("Adet") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    note, { note = it },
                    label = { Text("Not (sipariş no, tedarikçi…)") }, singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = {
                    val n = amount.toIntOrNull() ?: 0
                    val delta = if (isIn) n else -n
                    val default = if (isIn) "Giriş" else "Çıkış"
                    if (n <= 0) error = "Geçerli bir adet girin."
                    else if (!vm.changeStock(p, delta, note.ifBlank { default })) error = "Stok yetersiz."
                    else { amount = ""; note = "" }
                }) { Text("Hareketi kaydet") }

                HorizontalDivider()
                Text("Son hareketler", style = MaterialTheme.typography.titleSmall)
                if (history.isEmpty()) Text("Kayıt yok.", style = MaterialTheme.typography.bodySmall)
                Column(Modifier.heightIn(max = 240.dp)) {
                    history.forEach { m ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                (if (m.change > 0) "+" else "") + m.change,
                                fontWeight = FontWeight.Bold,
                                color = if (m.change > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.width(48.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(m.note, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    fmt.format(Date(m.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onEdit) { Text("Düzenle") }
            }
        },
    )
}
