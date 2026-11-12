# Retur Matcher (Android)

Aplikasi Android untuk mencocokkan nomor pesanan/nomor resi retur dengan data master Excel (.xlsx). Data disimpan lokal di perangkat menggunakan Room.

## Fitur yang tersedia pada source
- Import Excel melalui pemilih dokumen Android.
- Kolom wajib: nomor pesanan dan nomor resi; alias header dijelaskan di kode `ExcelService`.
- Penyimpanan data master dan riwayat scan lokal (Room).
- Input manual nomor pesanan atau resi.
- Status cocok, tidak ditemukan, dan duplikat.
- Scan barcode melalui Google Code Scanner.
- Ekspor laporan ke Excel.
- Ringkasan jumlah data dan riwayat scan.

## Build lokal
- Android Studio dengan Android Gradle Plugin 8.7.3.
- JDK 17, Android SDK 35, Gradle 8.9.
- Buka folder `ReturMatcher`, lalu jalankan Gradle Sync dan `assembleDebug`.

## Build APK otomatis di GitHub
Workflow `.github/workflows/android-apk.yml` berjalan saat push, pull request, atau `workflow_dispatch`.
Setelah workflow sukses: buka tab **Actions** → pilih run **Android APK** → unduh artifact **ReturMatcher-debug-apk**. APK debug dapat dipasang untuk uji coba; untuk distribusi publik/Play Store, buat APK/AAB release yang ditandatangani dengan keystore milik pemilik aplikasi.

## Catatan audit
Source awal adalah MVP dan belum membuktikan seluruh alur berjalan di perangkat. Uji import file Excel nyata, pemindaian kamera/Google Play Services, pencocokan, duplikat, ekspor, dan pemulihan data pada perangkat Android. Pencocokan saat ini exact match.
