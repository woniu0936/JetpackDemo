package com.novel.app.app.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

internal class NetworkMonitorImplApi24(
    private val context: Context,
    private val scope: CoroutineScope
) : NetworkMonitor {

    @RequiresApi(Build.VERSION_CODES.N)
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                channel.trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: android.net.NetworkCapabilities
            ) {
                val isConnected = networkCapabilities
                    .hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                channel.trySend(isConnected)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        channel.trySend(connectivityManager.activeNetwork != null)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = context.isCurrentlyConnected()
        )

}