package id.returmatcher.app.data

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object ExcelService {
    /** Header aliases supported: nomor pesanan/order/order number and nomor resi/resi/awb/tracking number. */
    fun importMaster(context: Context, uri: Uri): List<MasterOrder> {
        val input = context.contentResolver.openInputStream(uri) ?: error("File tidak dapat dibuka")
        return input.use { stream ->
            WorkbookFactory.create(stream).use { workbook ->
                require(workbook.numberOfSheets > 0) { "Workbook tidak memiliki sheet." }
                val sheet = workbook.getSheetAt(0)
                require(sheet.physicalNumberOfRows > 1) { "Excel kosong atau hanya berisi judul kolom" }
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                fun value(cell: org.apache.poi.ss.usermodel.Cell?): String =
                    cell?.let { formatter.formatCellValue(it, evaluator).trim() }.orEmpty()

                val header = sheet.getRow(0) ?: error("Baris judul kolom tidak ditemukan.")
                fun norm(v: String) = v.trim().lowercase().replace("_", " ").replace(Regex("\\s+"), " ")
                val headers = (0 until header.lastCellNum.coerceAtLeast(0)).associateBy { norm(value(header.getCell(it))) }
                fun col(vararg names: String): Int = names.firstNotNullOfOrNull { headers[it] }
                    ?: error("Kolom ${names.first()} tidak ditemukan")
                val orderCol = col("nomor pesanan", "no pesanan", "order", "order number", "nomor order")
                val resiCol = col("nomor resi", "no resi", "resi", "awb", "tracking number", "kode resi")
                val marketplaceCol = listOf("marketplace", "platform").firstNotNullOfOrNull { headers[it] }
                val productCol = listOf("produk", "nama produk", "product").firstNotNullOfOrNull { headers[it] }

                (1..sheet.lastRowNum).mapNotNull { index ->
                    val row = sheet.getRow(index) ?: return@mapNotNull null
                    val order = value(row.getCell(orderCol))
                    val resi = value(row.getCell(resiCol))
                    if (order.isBlank() && resi.isBlank()) null else MasterOrder(
                        orderNumber = order,
                        trackingNumber = resi,
                        marketplace = value(marketplaceCol?.let { row.getCell(it) }),
                        product = value(productCol?.let { row.getCell(it) })
                    )
                }
            }
        }
    }

    fun exportReport(context: Context, uri: Uri, master: List<MasterOrder>, scans: List<ScannedReturn>) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Hasil Retur")
            val headers = listOf("Nomor Pesanan", "Nomor Resi", "Marketplace", "Produk", "Status Retur")
            sheet.createRow(0).also { row ->
                headers.forEachIndexed { index, value -> row.createCell(index).setCellValue(value) }
            }
            val scanCodes = scans.map { it.scannedCode }.toSet()
            master.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                listOf(
                    item.orderNumber,
                    item.trackingNumber,
                    item.marketplace,
                    item.product,
                    if (item.orderNumber in scanCodes || item.trackingNumber in scanCodes)
                        "SUDAH DIPINDAI" else "BELUM DIPINDAI"
                ).forEachIndexed { column, value -> row.createCell(column).setCellValue(value) }
            }
            val start = master.size + 2
            sheet.createRow(start).createCell(0).setCellValue("HASIL SCAN")
            scans.forEachIndexed { index, scan ->
                val row = sheet.createRow(start + index + 1)
                row.createCell(0).setCellValue(scan.scannedCode)
                row.createCell(1).setCellValue(scan.result)
            }
            for (index in headers.indices) sheet.setColumnWidth(index, 6500)
            context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
                ?: error("Gagal menyimpan laporan")
        }
    }
}
