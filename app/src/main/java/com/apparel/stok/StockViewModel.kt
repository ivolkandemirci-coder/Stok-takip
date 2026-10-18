package com.apparel.stok

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apparel.stok.data.Movement
import com.apparel.stok.data.Product
import com.apparel.stok.data.StockDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = StockDatabase.get(app).dao()

    val query = MutableStateFlow("")
    val onlyLow = MutableStateFlow(false)

    val allProducts: StateFlow<List<Product>> = dao.products()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> =
        combine(allProducts, query, onlyLow) { list, q, low ->
            val term = q.trim()
            list.filter { p ->
                (!low || p.isLow) &&
                    (term.isEmpty() || listOf(p.name, p.sku, p.category, p.size, p.color)
                        .any { it.contains(term, ignoreCase = true) })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun movements(productId: Long): Flow<List<Movement>> = dao.movements(productId)

    fun save(product: Product) = viewModelScope.launch {
        if (product.id == 0L) dao.insertWithOpening(product)
        else dao.update(product.copy(updatedAt = System.currentTimeMillis()))
    }

    fun delete(product: Product) = viewModelScope.launch { dao.delete(product) }

    /** Stok eksiye düşecekse false döner. */
    fun changeStock(product: Product, delta: Int, note: String): Boolean {
        if (delta == 0 || product.quantity + delta < 0) return false
        viewModelScope.launch { dao.changeStock(product.id, delta, note) }
        return true
    }
}
