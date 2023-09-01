package com.vpnload.vpn.network

interface RequestResponseCallback {
    fun callback(data: String)
    fun callbackError()
}