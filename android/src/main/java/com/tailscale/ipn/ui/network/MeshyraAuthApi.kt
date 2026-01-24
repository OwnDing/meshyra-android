// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.network

import com.tailscale.ipn.ui.config.MeshyraBackendConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MeshyraApiResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: T? = null,
)

@Serializable
data class CaptchaResponseData(
    val captchaId: String,
    val imageBase64: String,
    val expiresIn: Long,
)

@Serializable
data class DeviceLoginRequest(
    val username: String,
    val password: String,
    val captchaCode: String,
    val captchaId: String,
)

@Serializable
data class DeviceLoginResponseData(
    val authKey: String,
    val serverUrl: String,
)

class MeshyraApiException(message: String, val code: Int? = null) : Exception(message)

object MeshyraAuthApi {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun fetchCaptcha(): Result<CaptchaResponseData> {
    val url = "${MeshyraBackendConfig.AUTH_BASE_URL}/api/auth/captcha"
    return requestRaw(method = "GET", url = url, body = null).flatMap { body ->
      decodeApiResponse<CaptchaResponseData>(body)
    }
  }

  suspend fun deviceLogin(request: DeviceLoginRequest): Result<DeviceLoginResponseData> {
    val url = "${MeshyraBackendConfig.AUTH_BASE_URL}/api/auth/device-login"
    val body = json.encodeToString(request).toByteArray(Charsets.UTF_8)
    return requestRaw(method = "POST", url = url, body = body).flatMap { respBody ->
      decodeApiResponse<DeviceLoginResponseData>(respBody)
    }
  }

  private suspend fun requestRaw(method: String, url: String, body: ByteArray?): Result<ByteArray> =
      withContext(Dispatchers.IO) {
        try {
          val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
              doOutput = true
              setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
          }

          if (body != null) {
            connection.outputStream.use { it.write(body) }
          }

          val responseCode = connection.responseCode
          val stream =
              if (responseCode in 200..299) connection.inputStream else connection.errorStream
          val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
          Result.success(bytes)
        } catch (t: Throwable) {
          Result.failure(t)
        }
      }

  private inline fun <reified T> decodeApiResponse(body: ByteArray): Result<T> {
    return try {
      val parsed = json.decodeFromString<MeshyraApiResponse<T>>(body.toString(Charsets.UTF_8))
      val data = parsed.data
      if (!parsed.success || parsed.code != 0 || data == null) {
        Result.failure(MeshyraApiException(parsed.message, parsed.code))
      } else {
        Result.success(data)
      }
    } catch (t: Throwable) {
      Result.failure(t)
    }
  }
}

private inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(onSuccess = transform, onFailure = { Result.failure(it) })

