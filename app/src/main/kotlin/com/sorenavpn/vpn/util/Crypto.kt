package com.sorenavpn.vpn.util

import android.util.Base64


class Crypto {
    fun encrypt(data: String): String {
        var base64 = Base64.encode(data.toByteArray(), 1)
        var result = ""
        var result2 = ""
        for (d in base64.toString()) {
            var code = d.code
            code += 1
            if (code == 128) code = 1
            result += code.toChar()
        }
        for (r in result) {
            var code = r.code
            code += 1
            if (code == 128) code = 1
            result2 += code.toChar()
        }
        return result2
    }

    fun decrypt(data: String): String {
        var result = ""
        var result2 = ""
        for (d in data) {
            var code = d.code
            if (code == 1) code = 128
            else code -= 1
            result += code.toChar()
        }
        for (r in result) {
            var code = r.code
            if (code == 1) code = 128
            else code -= 1
            result2 += code.toChar()
        }
        var bresult = Base64.decode(result2, 1)
        return String(bresult, Charsets.UTF_8)
    }
}