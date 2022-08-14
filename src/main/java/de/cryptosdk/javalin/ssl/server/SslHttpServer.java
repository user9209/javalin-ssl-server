package de.cryptosdk.javalin.ssl.server;

import de.cryptosdk.javalin.ssl.server.context.SslContextForJavalin;
import de.cryptosdk.javalin.ssl.server.exceptions.SkipOtherSteps;
import de.cryptosdk.javalin.ssl.server.handler.BasicHandler;
import de.cryptosdk.javalin.ssl.server.handler.IndexHandler;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.util.log.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.get;

public class SslHttpServer {


    public static int PORT_HTTP = -1; //50080;
    public static int PORT_HTTPS = 50443; //-1;

    public static void main(String ... args) {
        boolean tls_13_only = true;

        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

        Javalin app = Javalin.create(config -> {
            config.server(() -> SslContextForJavalin.getServer(PORT_HTTP, PORT_HTTPS, tls_13_only));
            if(Files.exists(Path.of("data/module/api/htmlStatic"))) {
                config.addStaticFiles("data/module/api/htmlStatic", Location.EXTERNAL);
            }

            if(new File(JAR_LOCATION + "static").exists()) {
                config.addStaticFiles(JAR_LOCATION + "static", Location.EXTERNAL);
            } else {
                System.out.println("No static content!");
            }
            config.enforceSsl = true;
            config.maxRequestSize = 4096000L;
        }).start();

        app.routes(() -> {
            before(BasicHandler.beforeHandler);
            get("/", IndexHandler.handle);
        });

        app.exception(SkipOtherSteps.class, (e, ctx) -> {});

        app.error(404, BasicHandler.notFound);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        app.events(event -> {
            event.serverStopping(() -> {
                System.out.println("Server is stopping ...");
            });
            event.serverStopped(() -> { System.out.println("Server is stopped successfully!"); });
        });
    }

    private static class NoLogging implements Logger {
        @Override public String getName() { return "no"; }
        @Override public void warn(String msg, Object... args) { }
        @Override public void warn(Throwable thrown) { }
        @Override public void warn(String msg, Throwable thrown) { }
        @Override public void info(String msg, Object... args) { }
        @Override public void info(Throwable thrown) { }
        @Override public void info(String msg, Throwable thrown) { }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public void setDebugEnabled(boolean enabled) { }
        @Override public void debug(String msg, Object... args) { }

        @Override
        public void debug(String msg, long value) {}

        @Override public void debug(Throwable thrown) { }
        @Override public void debug(String msg, Throwable thrown) { }
        @Override public Logger getLogger(String name) { return this; }
        @Override public void ignore(Throwable ignored) { }
    }

    public static final String JAR_LOCATION = setJarLocation();

    private static String setJarLocation() {
        URL jarLocationUrl = SslHttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return new File(jarLocationUrl.toString()).getParentFile().toString().substring(5) + File.separatorChar;
    }
}
