package com.shimuli.services

import com.shimuli.model.CardRequest
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec


object CardService {
    private lateinit var client: WebClient

    fun init(vertx: Vertx) {
        val options = WebClientOptions()
            .setSsl(true)
            .setTrustAll(true)
            .setVerifyHost(false)

        client = WebClient.create(vertx, options)
    }

    fun sendCardToExternalApi(cardRequest: CardRequest): Future<String> {
        val promise = io.vertx.core.Promise.promise<String>()

        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6ImI4ZTk1MTAzLWRmYWEtNDU5Ny1hZWEzLTlkNTY1Y2YyYjhjMSIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWUiOiJhZG1pbkBhZG1pbi5jb20iLCJDb21wYW55SWQiOiIxIiwidXNlcmlkIjoiYjhlOTUxMDMtZGZhYS00NTk3LWFlYTMtOWQ1NjVjZjJiOGMxIiwiZXhwIjoxNzQ3ODEyMjQyLCJpc3MiOiJHb2FsTWFya1Byb2plY3RzIiwiYXVkIjoiR29hbE1hcmtQcm9qZWN0cyJ9.7Wj6Gq2e1l1d0KLrrNSBZhLQllvYYzZtpt0vYL6SzWU"
        client.postAbs("https://localhost:7265/api/cards/createcard")
            .putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Bearer $token")
            .`as`(BodyCodec.string())
            .sendJson(cardRequest) { ar ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    if (response.statusCode() in 200..299) {
                        promise.complete(response.body())
                    } else {
                        val errorDetails = "API responded with ${response.statusCode()} - ${response.body()}"
                        println("⚠️ External API error: $errorDetails")
                        promise.fail(errorDetails)
                    }
                } else {
                    val cause = ar.cause()
                    println("Failed to call external API: ${cause.message}")
                    promise.fail("API request failed: ${cause.message}")
                }
            }

        return promise.future()
    }
}