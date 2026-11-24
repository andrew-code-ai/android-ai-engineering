/*
 * CartViewModel.kt — ekran koszyka
 *
 * Zadanie: zrefaktoruj ten kod zgodnie z zasadami, które sam wyznajesz
 * (architektura Androida — Clean Architecture, warstwy, unidirectional
 * data flow, testowalność ViewModelu). Komentuj decyzje na głos.
 * Nie musisz kompilować całości — liczy się kierunek, granice warstw
 * i uzasadnienie.
 */
package com.example.shop.cart

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
// ShopApi, CartResponse, CartItemDto — stuby na końcu pliku

class CartItem(
    val id: String,
    val name: String,
    var quantity: Int,
    val unitPriceCents: Int,
    val category: String
)

class CartViewModel(
    private val retrofit: Retrofit
) : ViewModel() {

    val items: MutableList<CartItem> = mutableListOf()

    var totalText: String = ""
    var discountText: String = ""
    var isLoading: Boolean = false
    var errorText: String = ""
    var canCheckout: Boolean = false

    private var promoCode: String = ""

    private val api: ShopApi = retrofit.create(ShopApi::class.java)

    fun loadCart(userId: String) {
        isLoading = true
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = withContext(Dispatchers.IO) { api.getCart(userId) }
                items.clear()
                for (dto in response.items) {
                    items.add(
                        CartItem(
                            id = dto.id,
                            name = dto.name,
                            quantity = dto.qty,
                            unitPriceCents = dto.priceCents,
                            category = dto.category
                        )
                    )
                }
                recalculate()
            } catch (e: Exception) {
                errorText = "Coś poszło nie tak"
            } finally {
                isLoading = false
            }
        }
    }

    fun increaseQuantity(itemId: String) {
        val item = items.firstOrNull { it.id == itemId } ?: return
        item.quantity = item.quantity + 1
        recalculate()
    }

    fun decreaseQuantity(itemId: String) {
        val item = items.firstOrNull { it.id == itemId } ?: return
        if (item.quantity > 0) {
            item.quantity = item.quantity - 1
        }
        recalculate()
    }

    fun applyPromo(code: String) {
        promoCode = code
        recalculate()
    }

    private fun recalculate() {
        var total = 0.0
        for (item in items) {
            total += (item.unitPriceCents / 100.0) * item.quantity
        }

        var discount = 0.0
        if (promoCode == "SUPER10") {
            discount = total * 0.10
        } else if (promoCode == "VIP") {
            var electronics = 0.0
            for (item in items) {
                if (item.category == "electronics") {
                    electronics += (item.unitPriceCents / 100.0) * item.quantity
                }
            }
            discount = electronics * 0.15
        }

        val shipping = if (total - discount >= 200.0) 0.0 else 15.0
        val grandTotal = total - discount + shipping

        totalText = "Razem: " + grandTotal + " zł"
        discountText = "Rabat: " + discount + " zł"

        var count = 0
        for (item in items) {
            count += item.quantity
        }
        canCheckout = count > 0 && grandTotal > 0
    }

    fun checkout(userId: String, onDone: (Boolean) -> Unit) {
        if (!canCheckout) {
            onDone(false)
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    api.checkout(userId, promoCode)
                }
                onDone(true)
            } catch (e: Exception) {
                errorText = "Checkout nieudany"
                onDone(false)
            }
        }
    }
}

// ---- Stuby (żeby się kompilowało) ----
interface ShopApi {
    suspend fun getCart(userId: String): CartResponse
    suspend fun checkout(userId: String, promo: String)
}
class CartResponse(val items: List<CartItemDto>)
class CartItemDto(
    val id: String, val name: String, val qty: Int,
    val priceCents: Int, val category: String
)
