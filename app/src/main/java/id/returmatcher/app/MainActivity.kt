package id.returmatcher.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import id.returmatcher.app.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val dao by lazy { ReturnDatabase.get(this).dao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ReturScreen() } }
    }

    @Composable
    private fun ReturScreen() {
        var code by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("Impor Excel data master untuk mulai.") }
        var masterCount by remember { mutableIntStateOf(0) }
        var matched by remember { mutableIntStateOf(0) }
        var missing by remember { mutableIntStateOf(0) }
        var history by remember { mutableStateOf(emptyList<ScannedReturn>()) }

        fun updateSummary() {
            lifecycleScope.launch {
                masterCount = dao.masterCount()
                matched = dao.matchedCount()
                missing = dao.missingCount()
                history = dao.allScans()
            }
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ) { uri: Uri? ->
            uri?.let { target ->
                lifecycleScope.launch {
                    runCatching {
                        ExcelService.exportReport(this@MainActivity, target, dao.allMaster(), dao.allScans())
                    }.onSuccess { message = "Laporan berhasil disimpan" }
                        .onFailure { message = "Gagal ekspor: ${it.message}" }
                }
            }
        }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { selected ->
                lifecycleScope.launch {
                    runCatching { ExcelService.importMaster(this@MainActivity, selected) }
                        .onSuccess { rows ->
                            if (rows.isEmpty()) {
                                message = "Tidak ada baris data valid pada Excel."
                            } else {
                                dao.clearMaster()
                                dao.insertMaster(rows)
                                message = "Berhasil impor ${rows.size} baris data master"
                                updateSummary()
                            }
                        }.onFailure { message = "Gagal impor: ${it.message}" }
                }
            }
        }

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("RETUR MATCHER", style = MaterialTheme.typography.headlineMedium)
            Text("Pencocokan retur berdasarkan data Excel", style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = {
                    importLauncher.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel"
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("IMPORT EXCEL DATA MASTER") }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Data master", masterCount.toString())
                Metric("Cocok", matched.toString())
                Metric("Tidak ada", missing.toString())
            }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Ketik nomor pesanan atau resi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    GmsBarcodeScanning.getClient(this@MainActivity).startScan()
                        .addOnSuccessListener { barcode ->
                            barcode.rawValue?.let { code = it }
                            if (barcode.rawValue.isNullOrBlank()) message = "Barcode tidak berisi teks yang dapat dibaca."
                        }
                        .addOnFailureListener { message = "Scan gagal: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("SCAN BARCODE DENGAN KAMERA") }

            Button(
                onClick = {
                    val value = code.trim()
                    if (value.isEmpty()) {
                        message = "Masukkan nomor pesanan atau resi terlebih dahulu."
                    } else {
                        lifecycleScope.launch {
                            runCatching {
                                val duplicate = dao.scanCount(value) > 0
                                val found = dao.findMaster(value)
                                val result = when {
                                    duplicate -> "DUPLIKAT"
                                    found != null -> "COCOK"
                                    else -> "TIDAK_DITEMUKAN"
                                }
                                dao.insertScan(ScannedReturn(
                                    scannedCode = value,
                                    matchedOrderId = found?.id,
                                    result = result
                                ))
                                result to found
                            }.onSuccess { (result, found) ->
                                message = when (result) {
                                    "COCOK" -> "COCOK: ${found?.orderNumber} / ${found?.trackingNumber}"
                                    "DUPLIKAT" -> "PERINGATAN: kode sudah pernah dipindai"
                                    else -> "RETUR TIDAK ADA DI DATA MASTER"
                                }
                                code = ""
                                updateSummary()
                            }.onFailure { message = "Gagal memeriksa retur: ${it.message}" }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("PERIKSA RETUR") }

            Text(message, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = { exportLauncher.launch("laporan_retur.xlsx") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("EKSPOR LAPORAN EXCEL") }

            Text("Riwayat scan", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.scannedCode) },
                        supportingContent = { Text(item.result) }
                    )
                }
            }
        }

        LaunchedEffect(Unit) { updateSummary() }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
