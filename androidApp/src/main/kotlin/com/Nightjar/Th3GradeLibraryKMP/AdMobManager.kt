package com.Nightjar.Th3GradeLibraryKMP

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {
    // Official Google AdMob Test Ad Unit IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var openCounter = 0
    private const val CAPPING_FREQUENCY = 3 // Show interstitial once every 3 book opens

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {}
            preloadInterstitial(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun preloadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialIfAllowed(activity: Activity, onAdDismissed: () -> Unit) {
        openCounter++
        if (openCounter % CAPPING_FREQUENCY == 0 && interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
}

@Composable
fun AdaptiveBannerAd(
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isAdLoaded,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
        ) + fadeIn(tween(250)),
        exit = shrinkVertically() + fadeOut()
    ) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    val displayMetrics = ctx.resources.displayMetrics
                    val adWidthPixels = displayMetrics.widthPixels.toFloat()
                    val density = displayMetrics.density
                    val adWidth = (adWidthPixels / density).toInt()
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                    adUnitId = AdMobManager.TEST_BANNER_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
