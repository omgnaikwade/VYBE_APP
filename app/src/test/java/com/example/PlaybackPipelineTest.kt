package com.example

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class PlaybackPipelineTest {

  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

  private fun decryptSaavnUrl(encryptedUrl: String): String {
    try {
      val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
      val secretKey = SecretKeySpec(keyBytes, "DES")
      val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey)
      val decoded = Base64.getDecoder().decode(encryptedUrl.trim())
      val decryptedBytes = cipher.doFinal(decoded)
      return String(decryptedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      return ""
    }
  }

  @Test
  fun testSaavnDecryption() {
    val queries = listOf("Tum Hi Ho", "Diljit Dosanjh", "Espresso Sabrina Carpenter", "Chaleya")

    for (q in queries) {
      try {
        val saavnUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=5&p=1&q=" + java.net.URLEncoder.encode(q, "UTF-8")
        val req = Request.Builder().url(saavnUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: ""
        if (resp.code == 200) {
          val json = JSONObject(body)
          val results = json.optJSONArray("results")
          if (results != null && results.length() > 0) {
            val s0 = results.getJSONObject(0)
            val title = s0.optString("title")
            val moreInfo = s0.optJSONObject("more_info")
            val enc = moreInfo?.optString("encrypted_media_url", "") ?: ""
            val decryptedUrl = decryptSaavnUrl(enc)
            println("Query '$q' -> Title: '$title', Decrypted Stream: ${decryptedUrl.take(70)}...")
            
            if (decryptedUrl.isNotBlank()) {
              // Test streaming reachability
              val headReq = Request.Builder().url(decryptedUrl).header("Range", "bytes=0-1024").build()
              val headResp = client.newCall(headReq).execute()
              println("Stream REACHABLE! code=${headResp.code}, contentType=${headResp.header("Content-Type")}, contentLength=${headResp.header("Content-Length")}")
            }
          }
        }
      } catch (e: Exception) {
        println("Error: ${e.message}")
      }
    }
  }
}
