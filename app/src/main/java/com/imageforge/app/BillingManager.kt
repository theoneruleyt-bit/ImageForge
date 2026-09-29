package com.imageforge.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Google Play one-time Lifetime Pro entitlement. Product id must match Play Console. */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    companion object { const val PRO_PRODUCT_ID = "imageforge_pro_lifetime" }

    data class State(
        val ready: Boolean = false,
        val isPro: Boolean = false,
        val localizedPrice: String? = null,
        val product: ProductDetails? = null,
        val message: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (client.isReady) { refresh(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(ready = true, message = null)
                    queryProduct(); refresh()
                } else _state.value = _state.value.copy(message = result.debugMessage)
            }
            override fun onBillingServiceDisconnected() { _state.value = _state.value.copy(ready = false) }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, response ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = response.productDetailsList.firstOrNull()
                val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                _state.value = _state.value.copy(product = details, localizedPrice = offer?.formattedPrice)
            }
        }
    }

    fun purchase(activity: Activity) {
        val product = _state.value.product ?: run { _state.value = _state.value.copy(message = "Pro product is not available yet. Check Play Store setup."); return }
        val offerToken = product.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken ?: run { _state.value = _state.value.copy(message = "No eligible Pro offer is available."); return }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offerToken).build()
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
    }

    fun restore() = refresh("Purchase status restored from Google Play.")

    private fun refresh(successMessage: String? = null) {
        if (!client.isReady) { start(); return }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchase = purchases.firstOrNull { PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                _state.value = _state.value.copy(isPro = purchase != null, message = successMessage)
                purchase?.let(::acknowledge)
            } else _state.value = _state.value.copy(message = result.debugMessage)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { if (it.purchaseState == Purchase.PurchaseState.PURCHASED) { _state.value = _state.value.copy(isPro = true, message = "ImageForge Pro unlocked."); acknowledge(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(message = "Purchase cancelled.")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh("ImageForge Pro is already owned.")
            else -> _state.value = _state.value.copy(message = result.debugMessage)
        }
    }

    fun close() { client.endConnection() }
}
