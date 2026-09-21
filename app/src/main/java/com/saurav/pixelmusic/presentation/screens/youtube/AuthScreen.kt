package com.saurav.pixelmusic.presentation.screens.youtube

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.saurav.pixelmusic.data.remote.youtube.Constants
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.eventFlow.collectLatest { event ->
            when (event) {
                AuthViewModel.ScreenEvent.Out.LoginCompleted -> {
                    Toast.makeText(context, "Successfully logged in!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")
                        authViewModel.onPageFinished(url)
                    }
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveDataSyncId(newDataSyncId: String?) {
                        if (newDataSyncId != null) {
                            val dataSyncId = newDataSyncId.substringBefore("||")
                            authViewModel.onDataSyncIdFound(dataSyncId)
                        }
                    }
                }, "Android")
                loadUrl(Constants.Auth.START_URL)
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        }
    )
}
