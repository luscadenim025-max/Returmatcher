# Retur Matcher (Android)

Aplikasi Android untuk mencocokkan nomor pesanan/nomor resi retur dengan data master Excel (.xlsx). Data disimpan lokal di perangkat menggunakan Room.

## Fitur
- Import Excel melalui pemilih dokumen Android.
- Alias header kolom wajib: nomor pesanan / no pesanan / order / order number / nomor order; dan nomor resi / no resi / resi / AWB / tracking number / kode resi.
- Menangani format tampilan sel Excel dan formula melalui Apache POI DataFormatter.
- Penyimpanan data master dan riwayat scan lokal (Room).
- Input manual nomor pesanan atau resi.
- Status cocok, tidak ditemukan, dan duplikat.
- Scan barcode melalui Google Code Scanner (membutuhkan Google Play Services yang kompatibel).
- Ekspor laporan Excel.
- Ringkasan jumlah data dan riwayat scan.

## Build
- JDK 17, Android SDK 35, Android Gradle Plugin 8.7.3.
- Gradle 8.9.
- Jalankan `gradle --no-daemon assembleDebug` dari root project.

## APK dari GitHub Actions
Setelah workflow **Android APK** sukses, buka tab **Actions**, pilih run terbaru, lalu unduh artifact **ReturMatcher-debug-apk**. APK debug untuk pengujian; distribusi publik memerlukan APK/AAB release yang ditandatangani dengan keystore pemilik aplikasi.

## Batasan yang perlu diuji
Aplikasi masih MVP. Sebelum dipakai operasional, uji dengan file Excel aktual (termasuk nomor resi dengan nol di depan), perangkat Android, kamera/Google Play Services, pencocokan, duplikat, ekspor, dan pemulihan data. Pencocokan menggunakan exact match setelah trim; belum melakukan fuzzy matching.
