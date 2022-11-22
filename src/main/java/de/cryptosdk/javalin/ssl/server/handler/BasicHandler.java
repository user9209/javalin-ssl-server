package de.cryptosdk.javalin.ssl.server.handler;

import io.javalin.http.Handler;
import io.javalin.http.util.NaiveRateLimit;

import java.util.concurrent.TimeUnit;

public class BasicHandler {
        public static Handler beforeHandler = (ctx) -> {
        ctx.res().setHeader("Server", "Hello World");

        NaiveRateLimit.requestPerTimeUnit(ctx, 600, TimeUnit.MINUTES);
    };

    public static Handler notFound = (ctx) -> {
        ctx.res().setHeader("Server","No such service");
        ctx.html("Nix da!");
        ctx.status(404);
    };
}
