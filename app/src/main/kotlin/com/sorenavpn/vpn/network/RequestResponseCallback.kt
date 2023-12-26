package com.sorenavpn.vpn.network

interface RequestResponseCallback {
    fun callback(data: String)
    fun callbackError()
}