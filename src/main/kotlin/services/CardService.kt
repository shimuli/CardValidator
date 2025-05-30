package com.shimuli.services

import com.shimuli.model.CardRequest
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.slf4j.LoggerFactory

object CardService {
    private lateinit var client: WebClient
    private val logger = LoggerFactory.getLogger(CardService::class.java)

    private var successfulSends = 0
    private var failedSends = 0


    fun init(vertx: Vertx) {
        val options = WebClientOptions()
            .setSsl(true)
            .setTrustAll(true)
            .setVerifyHost(false)

        client = WebClient.create(vertx, options)
        logger.info("CardService initialized")
    }


    fun sendCardToExternalApi(cardRequest: CardRequest): Future<String> {
        val promise = Promise.promise<String>()

        TokenService.getValidToken().onSuccess { token ->
            makeCardRequest(cardRequest, token, retryOn401 = true, promise)
        }.onFailure {
            val msg = "❌ Token retrieval failed: ${it.message}"
            logger.error(msg)
            failedSends++
            promise.fail(msg)
        }

        return promise.future()
    }

    private fun makeCardRequest(
        cardRequest: CardRequest,
        token: String,
        retryOn401: Boolean,
        promise: Promise<String>
    )
    {
        client.postAbs("https://localhost:7265/api/cards/createcard")
            .putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Bearer $token")
            .`as`(BodyCodec.string())
            .sendJson(cardRequest) { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    when (response.statusCode()) {
                        in 200..299 -> {
                            logger.info("✅ Card sent successfully. ${cardRequest.cardNumber} ")
                            successfulSends++
                            promise.complete(response.body())
                        }
                        401 -> {
                            logger.warn("⚠️ Unauthorized (401). Retrying with fresh token... ${cardRequest.cardNumber} ")
                            if (retryOn401) {
                                TokenService.getValidToken().onSuccess { newToken ->
                                    makeCardRequest(cardRequest, newToken, retryOn401 = false, promise)
                                }.onFailure {
                                    logger.error("❌ Token refresh failed during retry: ${it.message} - ${cardRequest.cardNumber} ")
                                    failedSends++
                                    promise.fail("Token refresh failed: ${it.message} - ${cardRequest.cardNumber} ")
                                }
                            } else {
                                logger.error("❌ Retry failed. Unauthorized access. - ${cardRequest.cardNumber} ")
                                failedSends++
                                promise.fail("Unauthorized (401) even after retry. - ${cardRequest.cardNumber} ")
                            }
                        }
                        else -> {
                            val msg = "❌ API responded with ${response.statusCode()} - ${response.body()}"
                            logger.error(msg)
                            failedSends++
                            promise.fail(msg)
                        }
                    }
                } else {
                    val msg = "❌ API call failed: ${ar.cause().message} ${cardRequest.cardNumber} "
                    logger.error(msg)
                    failedSends++
                    promise.fail(msg)
                }
            }
    }

    fun getMetrics(): Map<String, Int> {
        return mapOf(
            "successfulSends" to successfulSends,
            "failedSends" to failedSends
        )
    }
}