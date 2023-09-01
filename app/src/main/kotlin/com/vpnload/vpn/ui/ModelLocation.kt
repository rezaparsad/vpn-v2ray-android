package com.vpnload.vpn.ui

import com.vpnload.vpn.R

class ModelLocation {
    var country: String = "USA"
    var speed: String = "256 MS"
    var guid: String = ""
    var selected: Boolean = false

    fun getImage(): Int {
        return if (country.lowercase().contains("usa") || country.lowercase().contains("united_states")){
            R.drawable.flag_united_states
        }else if (country.lowercase().contains("united kingdom") || country.lowercase().contains("uk")) {
            R.drawable.flag_united_kingdom
        }else if (country.lowercase().contains("italy")) {
            R.drawable.flag_italy
        }else if (country.lowercase().contains("brazil")) {
            R.drawable.flag_brazil
        }else if (country.lowercase().contains("canada")) {
            R.drawable.flag_canada
        }else if (country.lowercase().contains("austria")) {
            R.drawable.flag_austria
        }else if (country.lowercase().contains("argentina")) {
            R.drawable.flag_argentina
        }else if (country.lowercase().contains("china")) {
            R.drawable.flag_china
        }else if (country.lowercase().contains("czech")) {
            R.drawable.flag_czech
        }else if (country.lowercase().contains("england")) {
            R.drawable.flag_england
        }else if (country.lowercase().contains("europe")) {
            R.drawable.flag_european
        }else if (country.lowercase().contains("japan")) {
            R.drawable.flag_japan
        }else if (country.lowercase().contains("korea")) {
            R.drawable.flag_south_korea
        }else if (country.lowercase().contains("poland")) {
            R.drawable.flag_poland
        }else if (country.lowercase().contains("russia")) {
            R.drawable.flag_russia
        }else if (country.lowercase().contains("spain")) {
            R.drawable.flag_spain
        }else if (country.lowercase().contains("sweden")) {
            R.drawable.flag_sweden
        }else if (country.lowercase().contains("turkey")) {
            R.drawable.flag_turkey
        }else if (country.lowercase().contains("ukraine")) {
            R.drawable.flag_ukraine
        }else if (country.lowercase().contains("australia")) {
            R.drawable.flag_australia
        }else if (country.lowercase().contains("colombia")) {
            R.drawable.flag_colombia
        }else if (country.lowercase().contains("denmark")) {
            R.drawable.flag_denmark
        }else if (country.lowercase().contains("finland")) {
            R.drawable.flag_finland
        }else if (country.lowercase().contains("france")) {
            R.drawable.flag_france
        }else if (country.lowercase().contains("india")) {
            R.drawable.flag_india
        }else if (country.lowercase().contains("malaysia")) {
            R.drawable.flag_malaysia
        }else if (country.lowercase().contains("mexico")) {
            R.drawable.flag_mexico
        }else if (country.lowercase().contains("morocco")) {
            R.drawable.flag_morocco
        }else if (country.lowercase().contains("netherlands")) {
            R.drawable.flag_netherlands
        }else if (country.lowercase().contains("norway")) {
            R.drawable.flag_norway
        }else if (country.lowercase().contains("peru")) {
            R.drawable.flag_peru
        }else if (country.lowercase().contains("philippines")) {
            R.drawable.flag_philippines
        }else if (country.lowercase().contains("romania")) {
            R.drawable.flag_romania
        }else if (country.lowercase().contains("singapore")) {
            R.drawable.flag_singapore
        }else if (country.lowercase().contains("switzerland")) {
            R.drawable.flag_switzerland
        }else if (country.lowercase().contains("thailand")) {
            R.drawable.flag_thailand
        }else if (country.lowercase().contains("venezuela")) {
            R.drawable.flag_venezuela
        }else if (country.lowercase().contains("vietnam")) {
            R.drawable.flag_vietnam
        }else if (country.lowercase().contains("germany")) {
            R.drawable.flag_germany
        } else {
            R.drawable.flag_united_states
        }
    }
}