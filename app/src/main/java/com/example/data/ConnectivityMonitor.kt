package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun observeNetworkConnectivity(context: Context): Flow<Boolean> =
  callbackFlow {
    val cm =
      context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        as? ConnectivityManager
    if (cm == null) {
      trySend(true)
      close()
      return@callbackFlow
    }

    fun checkCurrentlyOnline(): Boolean {
      return try {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      } catch (_: Exception) {
        true
      }
    }

    trySend(checkCurrentlyOnline())

    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          trySend(true)
        }

        override fun onLost(network: Network) {
          trySend(checkCurrentlyOnline())
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
          val online =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          trySend(online)
        }
      }

    try {
      val request =
        NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()
      cm.registerNetworkCallback(request, callback)
    } catch (_: Exception) {
      trySend(true)
    }

    awaitClose {
      try {
        cm.unregisterNetworkCallback(callback)
      } catch (_: Exception) {}
    }
  }.distinctUntilChanged()
