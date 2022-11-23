package de.cryptosdk.javalin.ssl.server.context;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SslContextForJavalin {

    private static final String pkcs12Keyfile = "localhost.p12";
    private static final String pkcs12Password = "1234";

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

    public static Server getServer(int portHttp, int portHttps, boolean tls_13_only) {
        Server server = new Server();
        ServerConnector sslConnector = null;
        ServerConnector connector = null;

        if(portHttps >= 0) {
            sslConnector = new ServerConnector(server, getSslContextFactory(tls_13_only));
            sslConnector.setPort(portHttps);
        }

        if(portHttp >= 0) {
            connector = new ServerConnector(server);
            connector.setPort(portHttp);
        }

        if(sslConnector == null) {
            server.setConnectors(new Connector[]{connector});
        } else if(connector == null) {
            server.setConnectors(new Connector[]{sslConnector});
        } else {
            server.setConnectors(new Connector[]{sslConnector, connector});
        }
        server.setErrorHandler(new FixedErrorHandler());
        return server;
    }

    private static SslContextFactory.Server getSslContextFactory(boolean tls_13_only) {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(SslContextForJavalin.class.getClassLoader().getResourceAsStream(pkcs12Keyfile), pkcs12Password.toCharArray());

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException();
        }
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword("1234");

        sslContextFactory.setUseCipherSuitesOrder(true);
        if(tls_13_only) {
            sslContextFactory.setIncludeProtocols("TLSv1.3");

            // Prefer CHACHA20 POLY1305 SHA256 then AES256 GCM SHA384
            sslContextFactory.setIncludeCipherSuites(
                "TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_256_GCM_SHA384"
            );
        }
        else {
            sslContextFactory.setIncludeProtocols("TLSv1.2","TLSv1.3");

            // Prefer CHACHA20 POLY1305 SHA256 then AES256 GCM SHA384
            sslContextFactory.setIncludeCipherSuites(
                // TLS 1.3
                "TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_256_GCM_SHA384",
                // TLS 1.2
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
            );
        }
        return sslContextFactory;
    }

    /*
        Handles error: ip or domain is called without valid cert for SNI
     */
    private static class FixedErrorHandler extends ErrorHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            LOG.warn("FixedErrorHandler for '" + baseRequest.getLocalName() + baseRequest.getOriginalURI() + "'");
            response.setStatus(500);
            response.setHeader("server","???");
            response.getWriter().println("<h1>Internal Server Error 500</h1>");
        }

        @Override
        public ByteBuffer badMessageError(int status, String reason, HttpFields.Mutable fields)
        {
            LOG.warn("BadMessageError " + status + " '" + reason + "'");
            return BufferUtil.toBuffer("<h1>Bad Message " + status + "</h1><pre>reason see logs!</pre>");
        }
    }
}
