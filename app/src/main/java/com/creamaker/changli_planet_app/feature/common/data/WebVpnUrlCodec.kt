package com.creamaker.changli_planet_app.feature.common.data

import android.net.Uri
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object WebVpnUrlCodec {
    private const val VPN_HOST = "vpn.csust.edu.cn"
    private const val PREFIX = "webvpn"
    private val key = "CASB2021EnLink!!".toByteArray(StandardCharsets.UTF_8)

    fun encrypt(raw: String): String {
        val normalized = normalize(raw)
        val uri = Uri.parse(normalized)
        val scheme = requireNotNull(uri.scheme) { "缺少 URL 协议" }
        val host = requireNotNull(uri.host) { "缺少主机名" }
        val hostWithPort = if (uri.port > 0) "$host:${uri.port}" else host
        val encryptedHost = crypt(Cipher.ENCRYPT_MODE, hostWithPort.toByteArray(StandardCharsets.UTF_8)).toHex()
        val path = uri.encodedPath?.takeIf { it.isNotBlank() } ?: "/"
        return Uri.Builder()
            .scheme("https")
            .authority(VPN_HOST)
            .encodedPath("/$scheme/$PREFIX$encryptedHost$path")
            .encodedQuery(uri.encodedQuery)
            .encodedFragment(uri.encodedFragment)
            .build()
            .toString()
    }

    fun decrypt(raw: String): String {
        val uri = Uri.parse(normalize(raw))
        require(uri.host == VPN_HOST) { "不是长沙理工大学 WebVPN 链接" }
        val segments = uri.pathSegments
        require(segments.size >= 2 && segments[1].startsWith(PREFIX)) { "WebVPN 链接格式不正确" }
        val scheme = segments[0]
        val encryptedHost = segments[1].removePrefix(PREFIX)
        val hostWithPort = String(crypt(Cipher.DECRYPT_MODE, encryptedHost.hexToBytes()), StandardCharsets.UTF_8)
        val hostParts = hostWithPort.split(":", limit = 2)
        val builder = Uri.Builder().scheme(scheme).authority(hostWithPort)
        segments.drop(2).forEach { builder.appendPath(it) }
        builder.encodedQuery(uri.encodedQuery).encodedFragment(uri.encodedFragment)
        return builder.build().toString().let { if (hostParts.size == 1 && segments.size == 2) it.removeSuffix("/") else it }
    }

    private fun normalize(raw: String): String {
        val value = raw.trim()
        require(value.isNotBlank()) { "请输入链接" }
        return when {
            value.startsWith("//") -> "http:$value"
            Uri.parse(value).scheme == null -> "http://$value"
            else -> value
        }
    }

    private fun crypt(mode: Int, input: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        return cipher.doFinal(input)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "密文长度无效" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
