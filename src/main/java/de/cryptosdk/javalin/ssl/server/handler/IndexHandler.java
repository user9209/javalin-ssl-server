package de.cryptosdk.javalin.ssl.server.handler;

import io.javalin.http.Handler;

public class IndexHandler {
    public static Handler handle = (ctx -> {
        ctx.html("<h1>Hello World! Server is running!</h1>");
    });
}
