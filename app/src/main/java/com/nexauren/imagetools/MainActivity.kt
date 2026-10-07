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
import com.nexauren.imagetools.ui.ImageToolsAppV5
import com.nexauren.imagetools.ui.theme.ImageToolsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val paymentSubscription = mutableStateOf<String?>(null)
    private val paymentRefreshNonce = mutableStateOf(0)

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
                if (auth.currentUser == null) {
                    premium = false
                } else {
                    firestore.observePremium { premium = it }
                }
            }

            LaunchedEffect(subscriptionId, auth.currentUser?.uid, paymentRefreshNonce.value) {
                if (auth.currentUser != null) {
                    scope.launch {
                        var activated = false
                        repeat(20) { attempt ->
                            val token = auth.idToken(attempt > 0)
                            if (!token.isNullOrBlank()) {
                                PaymentRepository.refreshSubscription(token, subscriptionId)
                                    .onSuccess { active ->
                                        if (active) {
                                            premium = true
                                            activated = true
                                        }
                                    }
                            }

                            if (activated) return@launch
                            if (attempt < 19) delay(3000)
                        }

                        if (!subscriptionId.isNullOrBlank()) {
                            paymentSubscription.value = null
                        }
                    }
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
                        scope.launch {
                            val token = auth.idToken()
                            if (!token.isNullOrBlank()) {
                                PaymentRepository.cancelSubscription(token)
                                    .onSuccess { cancelled ->
                                        if (cancelled) premium = false
                                    }
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

    override fun onResume() {
        super.onResume()
        paymentRefreshNonce.value += 1
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "imagetools" && uri.host == "paypal" && uri.path == "/return") {
            paymentSubscription.value = uri.getQueryParameter("subscriptionId")
                ?: uri.getQueryParameter("ba_token")
                ?: uri.getQueryParameter("orderId")
        }
    }
}
