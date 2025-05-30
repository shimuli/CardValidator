package com.shimuli.services

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.core.json.JsonObject
import java.time.Instant

object TokenService {
    private lateinit var client: WebClient
    private const val loginUrl = "https://localhost:7265/api/auth/login"
    private const val refreshUrl = "https://localhost:7265/api/auth/refresh"
    private const val email = "admin@admin.com"
    private const val password = "P@SSWord94"

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiry: Instant? = null

    fun init(vertx: Vertx) {
        val options = WebClientOptions()
            .setSsl(true)
            .setTrustAll(true)
            .setVerifyHost(false)

        client = WebClient.create(vertx, options)
    }

    fun getValidToken(): Future<String> {
        val now = Instant.now()
        return if (accessToken != null && tokenExpiry?.isAfter(now) == true) {
            Future.succeededFuture(accessToken)
        } else if (refreshToken != null) {
            refreshAccessToken()
        } else {
            login()
        }
    }

    private fun login(): Future<String> {
        val promise = Promise.promise<String>()
        val loginPayload = JsonObject()
            .put("email", email)
            .put("password", password)

        client.postAbs(loginUrl)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.jsonObject())
            .sendJsonObject(loginPayload) { ar ->
                if (ar.succeeded()) {
                    val body = ar.result().body()
                    accessToken = body.getString("token")
                    refreshToken = body.getString("refreshToken")

                    // Decode token expiry from JWT if needed, else assume 10 minutes
                    tokenExpiry = Instant.now().plusSeconds(600)

                    promise.complete(accessToken)
                } else {
                    promise.fail("Login failed: ${ar.cause().message}")
                }
            }

        return promise.future()
    }

    private fun refreshAccessToken(): Future<String> {
        val promise = Promise.promise<String>()
        val refreshPayload = JsonObject()
            .put("email", email)
            .put("refreshToken", refreshToken)

        client.postAbs(refreshUrl)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.jsonObject())
            .sendJsonObject(refreshPayload) { ar ->
                if (ar.succeeded()) {
                    val body = ar.result().body()
                    accessToken = body.getString("token")
                    refreshToken = body.getString("refreshToken")
                    tokenExpiry = Instant.now().plusSeconds(600)

                    promise.complete(accessToken)
                } else {
                    println("🔁 Token refresh failed: ${ar.cause().message}")
                    accessToken = null
                    refreshToken = null
                    tokenExpiry = null
                    promise.fail("Token refresh failed: ${ar.cause().message}")
                }
            }

        return promise.future()
    }


    fun startAutoRefresh(vertx: Vertx) {
        // Initial login on startup
        login().onSuccess {
            println("✅ Initial login successful")
        }.onFailure {
            println("❌ Initial login failed: ${it.message}")
        }

        // Set a periodic timer to refresh the token every 10 minutes
        vertx.setPeriodic(10 * 60 * 1000L) {
            refreshAccessToken().onSuccess {
                println("🔁 Token refreshed in background")
            }.onFailure {
                println("⚠️ Refresh failed in background: ${it.message}. Retrying login...")
                login().onSuccess {
                    println("✅ Re-login successful")
                }.onFailure { err ->
                    println("❌ Re-login failed: ${err.message}")
                }
            }
        }
    }

}

