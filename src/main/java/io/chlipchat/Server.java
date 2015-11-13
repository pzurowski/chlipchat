package io.chlipchat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.StringEscapeUtils;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Server extends AbstractVerticle {

    public static final String ADDRESS_CHAT_TO_SERVER = "chat.to.server";
    public static final String ADDRESS_CHAT_TO_CLIENT = "chat.to.client";
    public static final String ADDRESS_INIT = "init";

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_SERVER))
                .addOutboundPermitted(new PermittedOptions().setAddress(ADDRESS_CHAT_TO_CLIENT))
                .addOutboundPermitted(new PermittedOptions().setAddress(ADDRESS_INIT));


        List<String> messages = Collections.synchronizedList(new ArrayList<>());

        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts, event -> {


            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                messages.forEach(tmp -> {

                    String tmp2 ="EMPTY";
                    try {
                        tmp2 = StringEscapeUtils.escapeJava(tmp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String value = "{ \n" +
                            "  \"type\": \"rec\", \n" +
                            "  \"address\": \"" + ADDRESS_INIT + "\", \n" +
                            "  \"body\": \"" + tmp2 + "\"}";
                    event.socket().write(Buffer.buffer(value));
                });
            }

            event.complete(true);

        });


        router.route("/webjars/*").handler(webjarsHandler());
        router.route("/eventbus/*").handler(ebHandler);
        router.route().handler(staticHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        EventBus eb = vertx.eventBus();

        eb.consumer(ADDRESS_CHAT_TO_SERVER).handler(message -> {
            String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
            String value = "{ \"t\":\"" + timestamp + "\", \"p\":" + message.body() + "}";
            eb.publish(ADDRESS_CHAT_TO_CLIENT, value);
            messages.add(value);
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
