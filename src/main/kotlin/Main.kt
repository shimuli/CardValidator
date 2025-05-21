package com.shimuli
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    DatabindCodec.mapper().registerKotlinModule()
    val vertx = Vertx.vertx()
    vertx.deployVerticle(MainVerticle())
}