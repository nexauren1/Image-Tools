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
import com.nexauren.imagetools.ui.ImageToolsApp
import com.nexauren.imagetools.ui.theme.ImageToolsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val paymentOrder = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val auth = remember { AuthRepository(this@MainActivity) }
            val firestore = remember { FirestoreRepository() }
            var premium by remember { mutableStateOf(false) }
            var darkMode by remember { mutableStateOf(false) }
            val orderId = paymentOrder.value
            val scope = rememberCoroutineScope()

            LaunchedEffect(auth.currentUser?.uid) {
                if (auth.currentUser == null) {
                    premium = false
                } else {
                    firestore.syncUser()
                    firestore.observePremium { premium = it }
                }
            }

            LaunchedEffect(orderId, auth.currentUser?.uid) {
                if (!orderId.isNullOrBlank() && auth.currentUser != null) {
                    val token = auth.idToken()
                    if (!token.isNullOrBlank()) {
                        scope.launch {
                            if (PaymentRepository.captureOrder(token, orderId)) {
                                premium = true
                            }
                            paymentOrder.value = null
                        }
                    }
                }
            }

            ImageToolsTheme(darkTheme = darkMode) {
                ImageToolsApp(
                    auth = auth,
                    firestore = firestore,
                    premium = premium,
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it },
                    onStartPayment = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            paymentOrder.value = uri.getQueryParameter("orderId")
        }
    }
}