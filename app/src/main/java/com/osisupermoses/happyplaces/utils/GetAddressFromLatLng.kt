package com.osisupermoses.happyplaces.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask.execute
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


// Create a AsyncTask class fot getting an address from the latitude and longitude from the location provider.)
// START
/**
 * A AsyncTask class to get the address from latitude and longitude.
 */
class GetAddressFromLatLng(
    context: Context, private val latitude: Double,
    private val longitude: Double
): CoroutineScope {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    fun cancel() {
        job.cancel()
    }

    private fun onPostExecute(resultString: String?) {
        if (resultString == null) {
            mAddressListener.onError()
        } else {
            mAddressListener.onAddressFound(resultString)
        }
    }

    private fun execute() = launch {
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        onPostExecute(result)
    }
    /**
     * Constructs a Geocoder whose responses will be localized for the
     * given Locale.
     *
     * @param context the Context of the calling Activity
     * @param locale the desired Locale for the query results
     *
     * @throws NullPointerException if Locale is null
     */
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    /**
     * A variable of address listener interface.
     */
    private lateinit var mAddressListener: AddressListener

    private suspend fun doInBackground(): String =
        //(You can code here what you wants to execute in background execution without freezing the UI
        withContext(Dispatchers.IO) {
            try {
                /**
                 * Returns an array of Addresses that are known to describe the
                 * area immediately surrounding the given latitude and longitude.
                 */
                /**
                 * Returns an array of Addresses that are known to describe the
                 * area immediately surrounding the given latitude and longitude.
                 */
                val addressList: MutableList<Address>? = geocoder.getFromLocation(
                    latitude, longitude, 1)

                if (addressList != null && addressList.isNotEmpty()) {
                    val address: Address = addressList[0]
                    val sb = StringBuilder()
                    for (i in 0..address.maxAddressLineIndex) {
                        sb.append(address.getAddressLine(i)).append(",")
                    }
                    sb.deleteCharAt(sb.length - 1)// Here we remove the last comma that we have added above from the address.
                    return@withContext sb.toString()
                }
            } catch (e: IOException) {
                Log.e("HappyPlaces", "Unable connect to Geocoder")
            }
            return@withContext ""
        }

    /**
     * A public function to execute the AsyncTask(though in this case a coroutine) from the class is it called.
     */
    fun getAddress() {
        execute()
    }

    /**
     * A public function to set the AddressListener.
     */
    fun setAddressListener(addressListener: AddressListener) {
        mAddressListener = addressListener

    }

    /**
     * A interface for AddressListener which contains the function like success and error.
     */
    interface AddressListener {
        fun onAddressFound(address: String?)
        fun onError()
    }
}