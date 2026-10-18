package com.apparel.stok.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Bir ürün varyantı: örn. "Basic Tişört / M / Siyah" */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String = "",
    val size: String = "",
    val color: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Uygulama içinde saklanan fotoğrafın dosya yolu */
    val imagePath: String? = null,
) {
    val isLow: Boolean get() = quantity <= minQuantity
}

/** Stok hareketi: + giriş, - çıkış */
@Entity(
    tableName = "movements",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("productId")],
)
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val change: Int,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface StockDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE, size, color")
    fun products(): Flow<List<Product>>

    @Query("SELECT * FROM movements WHERE productId = :productId ORDER BY createdAt DESC LIMIT 50")
    fun movements(productId: Long): Flow<List<Movement>>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Insert
    suspend fun insertMovement(movement: Movement)

    @Query("UPDATE products SET quantity = quantity + :delta, updatedAt = :now WHERE id = :id")
    suspend fun adjust(id: Long, delta: Int, now: Long)

    @Transaction
    suspend fun changeStock(productId: Long, delta: Int, note: String) {
        adjust(productId, delta, System.currentTimeMillis())
        insertMovement(Movement(productId = productId, change = delta, note = note))
    }

    @Transaction
    suspend fun insertWithOpening(product: Product) {
        val id = insert(product)
        if (product.quantity > 0) {
            insertMovement(Movement(productId = id, change = product.quantity, note = "Açılış stoğu"))
        }
    }
}

@Database(entities = [Product::class, Movement::class], version = 2, exportSchema = false)
abstract class StockDatabase : RoomDatabase() {
    abstract fun dao(): StockDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
            }
        }

        @Volatile private var instance: StockDatabase? = null

        fun get(context: Context): StockDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stok.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
