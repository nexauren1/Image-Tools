package com.nexauren.imagetools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.nexauren.imagetools.auth.AuthRepository
import com.nexauren.imagetools.data.FirestoreRepository
import com.nexauren.imagetools.data.PaymentRepository
import com.nexauren.imagetools.data.SubscriptionStore
import com.nexauren.imagetools.ui.ImageToolsAppV5
import com.nexauren.imagetools.ui.theme.ImageToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val paymentSubscription = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val auth = remember { AuthRepository(this@MainActivity) }
            val firestore = remember { FirestoreRepository() }
            var premium by remember { mutableStateOf(false) }
            var darkMode by remember { mutableStateOf(false) }
            val subscriptionId = paymentSubscription.value
            val scope = rememberCoroutineScope()

            LaunchedEffect(auth.currentUser?.uid) {
                premium = false
                if (auth.currentUser != null) {
                    firestore.observePremium { premium = it }
                }
            }

            DisposableEffect(auth.currentUser?.uid) {
                onDispose { firestore.stop() }
            }

            // PayPal is reconciled only after returning from checkout.
            // Normal launches do not re-create a deleted Firestore entitlement.
            LaunchedEffect(subscriptionId, auth.currentUser?.uid) {
                if (auth.currentUser == null || subscriptionId.isNullOrBlank()) return@LaunchedEffect

                scope.launch {
                    var active = false
                    repeat(12) { attempt ->
                        val token = auth.idToken(attempt > 0)
                        if (!token.isNullOrBlank()) {
                            PaymentRepository.refreshSubscription(token, subscriptionId)
                                .onSuccess { active = it }
                        }
                        if (active) return@launch
                        if (attempt < 11) delay(3000)
                    }
                    paymentSubscription.value = null
                }
            }

            ImageToolsTheme(darkTheme = darkMode) {
                ImageToolsAppV5(
                    auth = auth,
                    premium = premium,
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it },
                    onStartPayment = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onCancelSubscription = {
                        val token = auth.idToken()
                        if (token.isNullOrBlank()) {
                            Result.failure(IllegalStateException("Please sign in again."))
                        } else {
                            PaymentRepository.cancelSubscription(
                                token,
                                SubscriptionStore.get(this@MainActivity) ?: paymentSubscription.value
                            ).onSuccess {
                                SubscriptionStore.clear(this@MainActivity)
                                paymentSubscription.value = null
                                firestore.refreshPremiumFromServer { premium = it }
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "imagetools" && uri.host == "paypal" && uri.path == "/return") {
            val id = uri.getQueryParameter("subscriptionId")
                ?: uri.getQueryParameter("ba_token")
                ?: uri.getQueryParameter("orderId")
            if (!id.isNullOrBlank()) {
                paymentSubscription.value = id
                SubscriptionStore.save(this, id)
            }
        }
    }
}
