package com.shimuli

import com.shimuli.handler.CardHandler
import com.shimuli.services.TokenService
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.ext.web.handler.TimeoutHandler

class MainVerticle : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {

        val router = Router.router(vertx)

        CardHandler.init(vertx)

        TokenService.init(vertx)
        TokenService.startAutoRefresh(vertx)


        router.route().handler(BodyHandler.create())
        router.route().handler(LoggerHandler.create())
        router.route().handler(ResponseTimeHandler.create())
        router.route().handler(TimeoutHandler.create(5_000))
        router.route().handler(BodyHandler.create())
        router.route().failureHandler {
            it.failure()?.printStackTrace()

            if(it.response().ended().not()){
                it.response().setStatusCode(500).end("Internal Server Error!!")
            }
        }


        router.get("/api/cards/validate").handler { handle ->
            handle.response().end("Hello from VISA card")
        }

        // card validation route
        router.post("/api/cards/validate").handler(CardHandler::validateCard)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(9000){ http ->
                if(http.succeeded()){
                    println("Server started at port 9000")
                    startPromise.complete()
                }
                else{
                    println("Failed to start HTTP server: ${http.cause().message}")
                    startPromise.fail(http.cause())
                }
            }
    }
}