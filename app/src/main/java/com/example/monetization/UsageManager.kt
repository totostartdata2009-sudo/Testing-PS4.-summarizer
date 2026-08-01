package com.example.monetization

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart Usage Manager
 * Centralized manager tracking AI usage limits, rewarded ads, premium status,
 * and interstitial ad eligibility using persistent local storage.
 */
class UsageManager private constructor(context: Context) {

    companion object {
        private const val TAG = "UsageManager"
        private const val PREFS_NAME = "ai_summary_monetization_prefs"

        const val MAX_FREE_SUMMARIES = 10
        const val MAX_REWARDED_ADS = 5
        const val SUMMARIES_PER_REWARDED_AD = 2

        private const val KEY_FREE_SUMMARIES_USED = "free_summaries_used"
        private const val KEY_REWARDED_SUMMARIES_REMAINING = "rewarded_summaries_remaining"
        private const val KEY_REWARDED_ADS_WATCHED = "rewarded_ads_watched_count"
        private const val KEY_IS_PREMIUM_USER = "is_premium_user"
        private const val KEY_INTERSTITIAL_TOGGLE = "interstitial_toggle_state"
        private const val KEY_TOTAL_SUMMARIES_COUNT = "total_summaries_count"

        @Volatile
        private var INSTANCE: UsageManager? = null

        fun getInstance(context: Context): UsageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Reactive StateFlows for UI updates
    private val _remainingSummariesFlow = MutableStateFlow(calculateRemainingSummaries())
    val remainingSummariesFlow: StateFlow<Int> = _remainingSummariesFlow.asStateFlow()

    private val _isPremiumFlow = MutableStateFlow(isPremiumUser())
    val isPremiumFlow: StateFlow<Boolean> = _isPremiumFlow.asStateFlow()

    private val _rewardedAdsWatchedFlow = MutableStateFlow(getRewardedAdsWatchedCount())
    val rewardedAdsWatchedFlow: StateFlow<Int> = _rewardedAdsWatchedFlow.asStateFlow()

    // Getters
    fun getFreeSummariesUsed(): Int = prefs.getInt(KEY_FREE_SUMMARIES_USED, 0)
    fun getRewardedSummariesRemaining(): Int = prefs.getInt(KEY_REWARDED_SUMMARIES_REMAINING, 0)
    fun getRewardedAdsWatchedCount(): Int = prefs.getInt(KEY_REWARDED_ADS_WATCHED, 0)
    fun isPremiumUser(): Boolean = prefs.getBoolean(KEY_IS_PREMIUM_USER, false)
    fun getTotalSummariesCount(): Int = prefs.getInt(KEY_TOTAL_SUMMARIES_COUNT, 0)

    fun getRemainingFreeSummaries(): Int {
        val used = getFreeSummariesUsed()
        return (MAX_FREE_SUMMARIES - used).coerceAtLeast(0)
    }

    private fun calculateRemainingSummaries(): Int {
        if (isPremiumUser()) return Int.MAX_VALUE
        return getRemainingFreeSummaries() + getRewardedSummariesRemaining()
    }

    /**
     * Checks if user is eligible to generate a summary.
     */
    fun canGenerateSummary(): Boolean {
        if (isPremiumUser()) return true
        return calculateRemainingSummaries() > 0
    }

    /**
     * Checks if user can watch more rewarded ads.
     * Maximum limit: 5 ads (10 summaries total).
     */
    fun canWatchRewardedAd(): Boolean {
        if (isPremiumUser()) return false
        return getRewardedAdsWatchedCount() < MAX_REWARDED_ADS
    }

    /**
     * Uses one summary slot.
     * Decrements rewarded summaries first if available; otherwise increments free summaries used.
     * Returns true if this summary was deducted from rewarded summaries (meaning NO forced ads).
     */
    fun useSummary(): Boolean {
        if (isPremiumUser()) {
            incrementTotalSummaries()
            updateFlows()
            return true // Premium user - no interstitial ads
        }

        val rewardedRemaining = getRewardedSummariesRemaining()
        var usedRewarded = false

        if (rewardedRemaining > 0) {
            prefs.edit().putInt(KEY_REWARDED_SUMMARIES_REMAINING, rewardedRemaining - 1).apply()
            usedRewarded = true
            Log.d(TAG, "Used 1 Rewarded summary. Remaining rewarded: ${rewardedRemaining - 1}")
        } else {
            val currentFreeUsed = getFreeSummariesUsed()
            if (currentFreeUsed < MAX_FREE_SUMMARIES) {
                prefs.edit().putInt(KEY_FREE_SUMMARIES_USED, currentFreeUsed + 1).apply()
                Log.d(TAG, "Used 1 Free summary. Total free used: ${currentFreeUsed + 1}/$MAX_FREE_SUMMARIES")
            }
        }

        incrementTotalSummaries()
        updateFlows()
        return usedRewarded
    }

    /**
     * Determines whether an Interstitial Ad should be shown for the current summary.
     * Rules:
     * - NO interstitial for Premium users.
     * - NO interstitial during rewarded summaries.
     * - Alternating pattern for free summaries: Show, Skip, Show, Skip, Show...
     */
    fun shouldShowInterstitial(wasRewardedSummary: Boolean): Boolean {
        if (isPremiumUser() || wasRewardedSummary) {
            return false
        }

        val currentToggle = prefs.getBoolean(KEY_INTERSTITIAL_TOGGLE, false)
        // Flip toggle for next time
        val nextToggle = !currentToggle
        prefs.edit().putBoolean(KEY_INTERSTITIAL_TOGGLE, nextToggle).apply()

        // Show on true, Skip on false (starts with Show on 1st summary)
        val shouldShow = !currentToggle
        Log.d(TAG, "Interstitial check: shouldShow=$shouldShow (next toggle will be $nextToggle)")
        return shouldShow
    }

    /**
     * Grants 2 extra summaries upon completely watching a Rewarded Ad.
     */
    fun grantRewardedAdBonus(): Boolean {
        val currentWatched = getRewardedAdsWatchedCount()
        if (currentWatched >= MAX_REWARDED_ADS) {
            Log.w(TAG, "Cannot grant rewarded bonus: Limit of $MAX_REWARDED_ADS reached.")
            return false
        }

        val newWatchedCount = currentWatched + 1
        val newRewardedRemaining = getRewardedSummariesRemaining() + SUMMARIES_PER_REWARDED_AD

        prefs.edit()
            .putInt(KEY_REWARDED_ADS_WATCHED, newWatchedCount)
            .putInt(KEY_REWARDED_SUMMARIES_REMAINING, newRewardedRemaining)
            .apply()

        Log.d(TAG, "Granted 2 rewarded summaries! Total watched: $newWatchedCount/$MAX_REWARDED_ADS")
        updateFlows()
        return true
    }

    /**
     * Sets user status to Premium (Golden Pro Purchase).
     */
    fun setPremiumStatus(isPremium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM_USER, isPremium).apply()
        Log.d(TAG, "Premium status updated: $isPremium")
        updateFlows()
    }

    private fun incrementTotalSummaries() {
        val count = getTotalSummariesCount()
        prefs.edit().putInt(KEY_TOTAL_SUMMARIES_COUNT, count + 1).apply()
    }

    private fun updateFlows() {
        _remainingSummariesFlow.value = calculateRemainingSummaries()
        _isPremiumFlow.value = isPremiumUser()
        _rewardedAdsWatchedFlow.value = getRewardedAdsWatchedCount()
    }
}
