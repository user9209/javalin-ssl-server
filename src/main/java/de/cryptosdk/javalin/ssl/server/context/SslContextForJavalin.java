package de.cryptosdk.javalin.ssl.server.context;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SslContextForJavalin {

    private static final String pkcs12Keyfile = "localhost.p12";
    private static final String pkcs12Password = "1234";

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
}
