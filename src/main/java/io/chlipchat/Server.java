package io.chlipchat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

public class Server extends AbstractVerticle {

    public static final String ADDRESS_CHAT_TO_SERVER = "chat.to.server";
    public static final String ADDRESS_CHAT_TO_CLIENT = "chat.to.client";

    @Override
    public void start() throws Exception {
        JsonArray messages = new JsonArray();

        Router router = Router.router(vertx);

        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_SERVER))
                .addOutboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_CLIENT));


        router.route("/webjars/*").handler(webjarsHandler());
        router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(opts));
        router.route().handler(staticHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        EventBus eb = vertx.eventBus();

        eb.consumer(ADDRESS_CHAT_TO_SERVER).handler(message -> {
            JsonObject body = (JsonObject) message.body();
            if("INIT".equals(body.getString("TYPE"))) {
                message.reply(messages);
            } else {
                body.put("instant", Instant.now());
                messages.add(body);
                eb.publish(ADDRESS_CHAT_TO_CLIENT, body);

            }
        });
    }

    private StaticHandler staticHandler() {
        return StaticHandler.create()
                .setCachingEnabled(false);
    }

    private StaticHandler webjarsHandler() {
        return StaticHandler.create()
                .setWebRoot("META-INF/resources/webjars")
                .setCachingEnabled(false);
    }
}
