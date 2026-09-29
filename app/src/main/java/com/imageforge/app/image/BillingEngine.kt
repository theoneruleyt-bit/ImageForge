package com.imageforge.app.image

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

/** Google Play one-time purchase manager for ImageForge Pro Lifetime. */
class BillingEngine(
    private val context: Context,
    private val onStateChanged: () -> Unit = {}
) : PurchasesUpdatedListener {
    companion object {
        const val PRO_PRODUCT_ID = "imageforge_pro_lifetime"
    }

    var isReady: Boolean = false
        private set
    var isPro: Boolean = false
        private set
    var formattedPrice: String? = null
        private set
    var statusMessage: String? = null
        private set
    private var productDetails: ProductDetails? = null
    private var selectedOfferToken: String? = null

    private val prefs = context.getSharedPreferences("imageforge_billing", Context.MODE_PRIVATE)
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        // Cached entitlement keeps the UI stable offline; Play is queried whenever a connection is available.
        isPro = prefs.getBoolean("pro_entitled", false)
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isReady = result.responseCode == BillingClient.BillingResponseCode.OK
                if (isReady) {
                    queryProduct()
                    restorePurchases(silent = true)
                } else {
                    statusMessage = "Google Play Billing is not available right now."
                    onStateChanged()
                }
            }

            override fun onBillingServiceDisconnected() {
                isReady = false
                onStateChanged()
            }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull()
                val offer = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                selectedOfferToken = offer?.offerToken
                formattedPrice = offer?.formattedPrice
            }
            onStateChanged()
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        val offerToken = selectedOfferToken
        if (!isReady || details == null || offerToken == null) {
            statusMessage = "Pro product is not available yet. Install the Play test build and check the Play Console product."
            onStateChanged()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            statusMessage = result.debugMessage.ifBlank { "Could not start purchase." }
            onStateChanged()
        }
    }

    fun restorePurchases(silent: Boolean = false) {
        if (!billingClient.isReady) {
            if (!silent) statusMessage = "Connecting to Google Play…"
            if (!isReady) connect()
            onStateChanged()
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(PRO_PRODUCT_ID)
                }
                setEntitlement(owned)
                purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        it.products.contains(PRO_PRODUCT_ID) && !it.isAcknowledged
                }.forEach(::acknowledge)
                if (!silent) statusMessage = if (owned) "ImageForge Pro restored." else "No Pro purchase was found for this Google Play account."
            } else if (!silent) {
                statusMessage = result.debugMessage.ifBlank { "Could not restore purchases." }
            }
            onStateChanged()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> statusMessage = "Purchase canceled."
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> statusMessage = result.debugMessage.ifBlank { "Purchase was not completed." }
        }
        onStateChanged()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(PRO_PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            setEntitlement(true)
            statusMessage = "ImageForge Pro Lifetime unlocked."
            if (!purchase.isAcknowledged) acknowledge(purchase)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                statusMessage = "Purchase received, but acknowledgement is pending."
            }
            onStateChanged()
        }
    }

    private fun setEntitlement(value: Boolean) {
        isPro = value
        prefs.edit().putBoolean("pro_entitled", value).apply()
    }

    fun close() = billingClient.endConnection()
}
