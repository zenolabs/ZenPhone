package app.baldphone.neo.features.notifications.data

import android.app.Notification
import android.content.Context
import android.os.Build
import android.provider.CallLog
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.util.Log

/**
 * Logic for classifying and filtering [StatusBarNotification] objects.
 */
object NotificationClassifier {
    private val KNOWN_DIALERS =
        setOf(
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.miui.dialer",
            "com.android.incallui",
            "com.huawei.contacts",
            "com.android.contacts"
        )

    private val KNOWN_MISSED_CHANNELS =
        setOf(
            "phone_missed_call",
            "missed_call",
            "call_missed",
            "TelecomMissedCalls" // Xiaomi
        )

    /**
     * Determines whether a given notification represents a missed call.
     */
    fun isMissedCall(context: Context, sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val pkg = sbn.packageName

        if (n == null) return false
        val fromDialer = isFromDialer(context, pkg)

        val extras = n.extras
        val callType = extras.getInt("android.callType", -1)
        val isMissedType = callType == CallLog.Calls.MISSED_TYPE
        val missedCallCount = extras.getInt("android.telecom.extra.MISSED_CALL_COUNT", 0)
        val hasMissedCount = missedCallCount > 0

        val isMissedCategory =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                n.category == Notification.CATEGORY_MISSED_CALL
            } else {
                n.category == "missed_call"
            }

        // Notification Channels
        val isKnownMissedChannel =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                KNOWN_MISSED_CHANNELS.contains(n.channelId)
            } else {
                false
            }

        val isMissedIndicator = isMissedType || hasMissedCount || isMissedCategory || isKnownMissedChannel
        val isNotOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) == 0

        Log.d(
            "NotificationClassifier",
            "isMissedCall: pkg=$pkg, " +
                "fromDialer=$fromDialer, " +
                "callType=$callType, " +
                "missedCallCount=$missedCallCount, " +
                "category=${n.category}, " +
                "channelId=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) n.channelId else "N/A"}, " +
                "isMissedIndicator=$isMissedIndicator, " +
                "isNotOngoing=$isNotOngoing, " +
                "extrasKeys=${extras.keySet().joinToString(",")}"
        )

        return fromDialer && isMissedIndicator && isNotOngoing
    }

    /**
     * Keeps the notifications worth putting in front of someone, out of everything on the phone.
     *
     * A group summary is a heading for the notifications under it - "3 new messages" standing
     * over the three - so showing it alongside them says everything twice. Dropping every
     * summary is not the same thing, though, and was the mistake here: an app that posts a
     * summary over a group of one, which messaging apps commonly do, had its notification
     * thrown away with nothing left to take its place. Missed calls survived only because the
     * dialer posts them singly, with no summary at all.
     *
     * So a summary goes only when the notifications it stands over are here to speak for
     * themselves. This has to look at the whole list at once, which is why it is not a question
     * that can be asked of one notification on its own.
     */
    fun keepWorthShowing(all: List<StatusBarNotification>): List<StatusBarNotification> {
        val groupsWithChildren =
            all
                .filter { it.notification != null && !it.isGroupSummary() }
                .map { it.groupKey }
                .toSet()

        return all.filter { sbn ->
            if (sbn.notification == null) return@filter false
            !sbn.isGroupSummary() || sbn.groupKey !in groupsWithChildren
        }
    }

    private fun StatusBarNotification.isGroupSummary(): Boolean =
        (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    /**
     * Checks if the given package name belongs to a dialer application.
     */
    private fun isFromDialer(context: Context, packageName: String): Boolean {
        if (KNOWN_DIALERS.any { packageName.startsWith(it) }) return true

        val defaultDialer =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                tm?.defaultDialerPackage
            } else {
                null
            }

        return packageName == defaultDialer
    }
}
