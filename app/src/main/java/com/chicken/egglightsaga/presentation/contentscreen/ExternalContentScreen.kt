package com.chicken.egglightsaga.presentation.contentscreen

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun ExternalContentScreen(
    url: String,
    onFilePicker: (Intent, ValueCallback<Array<Uri>>, Uri?) -> Unit
) {
    val context = LocalContext.current

    val customUserAgent = remember {
        WebSettings
            .getDefaultUserAgent(context)
            .replace("; wv", "")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val primaryContentState = remember { mutableStateOf<WebView?>(null) }
    val secondaryContentState = remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = true) {
        when {
            secondaryContentState.value != null -> {
                val popup = secondaryContentState.value
                val parent = popup?.parent as? FrameLayout
                parent?.removeView(popup)
                popup?.destroy()
                secondaryContentState.value = null
            }

            primaryContentState.value?.canGoBack() == true -> {
                primaryContentState.value?.goBack()
            }

            else -> {
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val popupContainer = FrameLayout(context)
            val root = FrameLayout(context)

            val mainWebView = ContentEngineProvider(
                context = context,
                userAgent = customUserAgent,
                popupContainer = popupContainer,
                onPopupCreated = { popup -> secondaryContentState.value = popup },
                onPopupClosed = { popup ->
                    popupContainer.removeView(popup)
                    secondaryContentState.value = null
                },
                onFilePicker = onFilePicker
            )

            primaryContentState.value = mainWebView

            root.addView(
                mainWebView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            root.addView(
                popupContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            try {
                val method = WebView::class.java.getMethod("loadUrl", String::class.java)
                method.invoke(mainWebView, url)
            } catch (_: Throwable) {
                mainWebView.loadUrl(url)
            }

            root
        },
        update = {

        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    primaryContentState.value?.onResume()
                    primaryContentState.value?.resumeTimers()
                    secondaryContentState.value?.onResume()
                    secondaryContentState.value?.resumeTimers()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    primaryContentState.value?.onPause()
                    primaryContentState.value?.pauseTimers()
                    secondaryContentState.value?.onPause()
                    secondaryContentState.value?.pauseTimers()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}