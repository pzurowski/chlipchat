package io.chlipchat;

import io.vertx.core.Vertx;

public class App {

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(Server.class.getName());
    }

}
