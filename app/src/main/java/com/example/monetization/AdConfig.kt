package com.example.monetization

import android.content.Context
import com.example.BuildConfig

/**
 * Centralized Ad Configuration for AppLovin MAX Mediation & Bidding Networks.
 * Networks configured:
 * 1. AppLovin MAX (Mediation Platform & Bidding Ad Network)
 * 2. Google AdMob
 * 3. Pangle
 * 4. Mintegral
 * 5. Liftoff Monetize (Vungle)
 *
 * Switch to production Ad Unit IDs by replacing test IDs below.
 */
object AdConfig {

    // AppLovin MAX SDK Key (Test Key / Placeholder)
    const val APPLOVIN_SDK_KEY = "YOUR_APPLOVIN_SDK_KEY_HERE_TEST_MODE"

    // Official Test Ad Unit IDs for AppLovin MAX
    // Replacing these with production IDs will automatically apply across the app.
    const val INTERSTITIAL_AD_UNIT_ID = "YOUR_INTERSTITIAL_AD_UNIT_ID"
    const val REWARDED_AD_UNIT_ID = "YOUR_REWARDED_AD_UNIT_ID"
    const val BANNER_AD_UNIT_ID = "YOUR_BANNER_AD_UNIT_ID"
    const val NATIVE_BANNER_AD_UNIT_ID = "YOUR_NATIVE_BANNER_AD_UNIT_ID"

    // Google AdMob Test App ID
    const val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * Determines whether test ads should be served based on Build type or forced configuration.
     */
    fun isDebugMode(): Boolean {
        return BuildConfig.DEBUG
    }
}
