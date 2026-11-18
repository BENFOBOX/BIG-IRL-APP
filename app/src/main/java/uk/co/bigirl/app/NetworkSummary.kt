package uk.co.bigirl.app
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
data class NetworkSummary(val wifi:Boolean,val cellular:Boolean)
fun readNetworkSummary(c:Context):NetworkSummary{val m=c.getSystemService(ConnectivityManager::class.java);var w=false;var x=false;m.allNetworks.forEach{n->val a=m.getNetworkCapabilities(n)?:return@forEach;if(a.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)){w=w||a.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);x=x||a.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}};return NetworkSummary(w,x)}