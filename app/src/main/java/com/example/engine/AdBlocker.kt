package com.example.engine

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val BLOCKED_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "google-analytics.com",
        "hotjar.com",
        "scorecardresearch.com",
        "segment.io",
        "mixpanel.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "advertising.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "moatads.com",
        "popads.net",
        "propellerads.com",
        "adcolony.com",
        "chartboost.com",
        "revcontent.com",
        "zergnet.com",
        "quantserve.com",
        "statcounter.com",
        "clarity.ms",
        "adroll.com",
        "yieldmo.com",
        "smartadserver.com",
        "amazon-adsystem.com",
        "adsystem.com",
        "casalemedia.com",
        "lijit.com",
        "adsafeprotected.com",
        "turn.com",
        "exponential.com",
        "zemanta.com",
        // YouTube ad and tracking endpoints
        "googleads.g.doubleclick.net",
        "ad.youtube.com",
        "youtube.com/api/stats/ads",
        "youtube.com/pagead",
        "youtube.com/ptracking",
        "static.doubleclick.net"
    )

    private val BLOCKED_PATH_KEYWORDS = listOf(
        "/ads.js",
        "/pagead/",
        "/adserver/",
        "/advertisement/",
        "/api/stats/ads",
        "/api/stats/qoe?adformat",
        "/youtubei/v1/att/get",
        "/get_midroll_info",
        "fbevents.js",
        "google-analytics.com/analytics.js",
        "googletagmanager.com/gtag/js"
    )

    const val YOUTUBE_ADBLOCK_SCRIPT = """
        (function() {
            // Nova Shield: YouTube Ad Neutralizer & Skip Engine
            function cleanYouTubeAds() {
                try {
                    // Click skip ad button if present
                    var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-slot button, [class*="skip-button"]');
                    skipButtons.forEach(function(btn) {
                        if (btn) btn.click();
                    });

                    // Hide overlay and banner ads
                    var adElements = document.querySelectorAll(
                        '.video-ads, .ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-player-overlay, ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ytd-banner-promo-renderer, ytd-in-feed-ad-layout-renderer, ytd-ad-slot-renderer, #player-ads, .ad-showing'
                    );
                    adElements.forEach(function(el) {
                        if (el && el.style.display !== 'none') {
                            el.style.display = 'none';
                        }
                    });

                    // If video player has ad playing, fast-forward to the end
                    var video = document.querySelector('video');
                    var isAd = document.querySelector('.ad-showing, .ad-interrupting');
                    if (video && isAd && !isNaN(video.duration) && video.duration > 0) {
                        video.currentTime = video.duration;
                    }
                } catch(e) {}
            }

            // Run continuously on video change
            if (!window.__nova_adblock_attached) {
                window.__nova_adblock_attached = true;
                setInterval(cleanYouTubeAds, 500);
            }
        })();
    """

    fun isAdOrTracker(url: String): Boolean {
        if (url.isBlank()) return false
        val cleanUrl = url.lowercase()
        return try {
            val uri = Uri.parse(cleanUrl)
            val host = uri.host ?: return false

            // Check known blocked host domains
            BLOCKED_DOMAINS.any { blockedDomain ->
                host == blockedDomain || host.endsWith(".$blockedDomain")
            } || BLOCKED_PATH_KEYWORDS.any { keyword ->
                cleanUrl.contains(keyword)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
