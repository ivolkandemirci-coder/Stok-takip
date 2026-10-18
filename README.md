# Stok Takip (Apparel)

Giyim ürünleri için basit, çevrimdışı çalışan Android stok takip uygulaması.
Kotlin + Jetpack Compose (Material 3) + Room.

## Özellikler
- Ürün/varyant ekleme: ad, SKU, kategori, beden (XS–XXL hızlı seçim), renk, açılış stoğu, kritik seviye
- Listede tek dokunuşla −1 / +1
- Ürüne dokununca: toplu giriş/çıkış (adet + not), son 50 hareket, düzenle, sil
- Kritik stoktaki ürünler kırmızı vurgulanır, "Sadece kritik stok" filtresi
- Ad, SKU, kategori, beden ve renkte arama
- Özet: ürün çeşidi, toplam adet, kritik ürün sayısı
- Stok eksiye düşmez; her değişiklik hareket geçmişine yazılır

## Çalıştırma
1. Android Studio (Ladybug veya üstü) ile `StokTakip` klasörünü açın (File → Open).
2. Gradle senkronizasyonunu bekleyin (wrapper ilk açılışta indirilir; JDK 17 gerekir).
3. Telefonu USB hata ayıklama ile bağlayın veya emülatör seçip ▶ Run'a basın.
4. APK için: Build → Build App Bundle(s) / APK(s) → Build APK(s).

Minimum Android 8.0 (API 26).

## Dosya yapısı
- `data/Data.kt` — Product, Movement tabloları, DAO ve veritabanı
- `StockViewModel.kt` — arama, filtre ve stok işlemleri
- `MainActivity.kt` — tüm ekranlar (liste, ürün formu, stok hareketi penceresi)
