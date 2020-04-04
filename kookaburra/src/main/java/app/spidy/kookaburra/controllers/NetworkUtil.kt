package app.spidy.kookaburra.controllers

import android.content.Context
import android.net.ConnectivityManager
import java.lang.Exception


object NetworkUtil {
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nwInfo = connectivityManager.activeNetworkInfo
            nwInfo != null && nwInfo.isConnectedOrConnecting
        } catch (e: Exception) {
            true
        }
    }
}