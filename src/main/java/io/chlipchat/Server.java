package io.chlipchat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.time.Instant;

public class Server extends AbstractVerticle {

    //TODO: need to have login

    public static final String ADDRESS_CHAT_TO_SERVER = "chat.to.server";
    public static final String ADDRESS_CHAT_TO_CLIENT = "chat.to.client";
    public static final String ADDRESS_MAINTENANCE = "chat.MAINTENANCE";
    public static final String USERS_MAP_NAME = "users";

    @Override
    public void start() throws Exception {
        JsonArray messages = new JsonArray();

        Router router = Router.router(vertx);

        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_SERVER))
                .addOutboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_CLIENT))
                .addOutboundPermitted(new PermittedOptions().setAddress(ADDRESS_MAINTENANCE));

        router.route("/webjars/*").handler(webjarsHandler());
        router.route("/eventbus/*").handler(sockJSHandler(opts));
        router.route().handler(staticHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        EventBus eb = vertx.eventBus();

        eb.consumer(ADDRESS_CHAT_TO_SERVER).handler(message -> {
            JsonObject body = (JsonObject) message.body();
            if("init".equals(body.getString("cmd"))) {
                message.reply(messages);
                eb.publish(ADDRESS_MAINTENANCE, new JsonObject()
                        .put("cmd", "userJoined")
                        .put("login", body.getString("login"))
                        .put("handlerID", body.getString("handlerID")));
                vertx.sharedData().getLocalMap(USERS_MAP_NAME)
                        .put(body.getString("handlerID"), new JsonObject()
                                .put("login", body.getString("login"))
                                .put("handlerID",body.getString("handlerID")));
            }
            else if("user-list".equals(body.getString("cmd"))) {
                JsonArray userList = new JsonArray();
                vertx.sharedData().getLocalMap(USERS_MAP_NAME).values()
                        .stream()
                        .forEach(user -> userList.add(user));
                message.reply(userList);
            }
            else {
                body.put("instant", Instant.now());
                messages.add(body);
                eb.publish(ADDRESS_CHAT_TO_CLIENT, body);
            }
        });
    }

    private SockJSHandler sockJSHandler(BridgeOptions opts) {
        return SockJSHandler.create(vertx).bridge(opts, event -> {
            if(event.type() == BridgeEventType.SOCKET_CREATED) {
                event.socket().write(Buffer.buffer(new JsonObject()
                        .put("type", "rec")
                        .put("body", new JsonObject()
                                .put("handlerID", event.socket().writeHandlerID())
                                .put("cmd", "handlerID")
                        )
                        .put("address", ADDRESS_MAINTENANCE).encode()));
            }
            if(event.type() == BridgeEventType.SOCKET_CLOSED) {
                vertx.eventBus().publish(ADDRESS_MAINTENANCE, new JsonObject()
                        .put("cmd", "userLeft")
                        .put("login", ((JsonObject) vertx.sharedData().getLocalMap(USERS_MAP_NAME).get(event.socket().writeHandlerID())).getString("login"))
                        .put("handlerID", event.socket().writeHandlerID()));
                vertx.sharedData().getLocalMap(USERS_MAP_NAME).remove(event.socket().writeHandlerID());
            }
            event.complete(true);
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
