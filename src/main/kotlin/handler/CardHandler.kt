package com.shimuli.handler
import com.shimuli.model.CardRequest
import com.shimuli.services.CardService
import com.shimuli.validation.CardValidator
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec

object CardHandler {

    fun init(vertx: Vertx) {
        CardService.init(vertx) // initialize WebClient once
    }

    fun validateCard(ctx: RoutingContext) {
        try {
            val requestJson = ctx.body().asJsonObject()
            val cardRequest = requestJson.mapTo(CardRequest::class.java)
            val result = CardValidator.validate(cardRequest)

            if (!result.valid) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodePrettily(result))
                return
            }

            // Send to external API only if card is valid
            CardService.sendCardToExternalApi(cardRequest).onComplete { ar ->
                if (ar.succeeded()) {
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encodePrettily(result))
                } else {
                    ctx.response()
                        .setStatusCode(502)
                        .putHeader("Content-Type", "application/json")
                        .end(
                            Json.encodePrettily(
                                mapOf(
                                    "error" to "Failed to reach external API",
                                    "details" to ar.cause().message
                                )
                            )
                        )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace() // Logs the error to the console

            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end("""{ "error": "Internal Server Error", "details": "${e.message}" }""")
        }
    }
}