package org.lantern;

import static org.junit.Assert.*;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.lantern.proxy.CertTrackingSSLEngineSource;
import org.lantern.state.Model;
import org.lantern.util.HttpClientFactory;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class SslHttpProxyServerTest {

    @Test
    public void test() throws Exception {
        //System.setProperty("java.net.preferIPv4Stack", "true");
        Launcher.configureCipherSuites();
        // LanternUtils.setFallbackProxy();
        System.setProperty("javax.net.debug", "ssl");
        org.jboss.netty.util.Timer timer =
                new org.jboss.netty.util.HashedWheelTimer();

        final LanternKeyStoreManager ksm = new LanternKeyStoreManager();
        final LanternTrustStore ts = new LanternTrustStore(ksm);
        final String testId = "127.0.0.1";// "test@gmail.com/somejidresource";
        ts.addBase64Cert(new URI(testId), ksm.getBase64Cert(testId));
        CertTrackingSSLEngineSource cses = new CertTrackingSSLEngineSource(ts,
                ksm);
        final int port = LanternUtils.randomPort();
        final Model model = new Model();
        model.getSettings().setServerPort(port);

        final HttpProxyServer server = DefaultHttpProxyServer.bootstrap()
                .withPort(port)
                .withSSLEngineSource(cses)
                .withAllowLocalOnly(true)
                .withListenOnAllAddresses(true)
                .start();

        LanternUtils.waitForServer(port);
        
        try {

            final LanternSocketsUtil socketsUtil =
                    new LanternSocketsUtil(null, ts);
            final HttpClientFactory httpFactory =
                    new HttpClientFactory(socketsUtil, null, null);

            final HttpHost host = new HttpHost(
                    "127.0.0.1", port, "https");

            final HttpClient client = httpFactory.newClient(host, true);

            final HttpGet get = new HttpGet("https://www.google.com");

            final HttpResponse response = client.execute(get);
            final HttpEntity entity = response.getEntity();
            final String body =
                    IOUtils.toString(entity.getContent()).toLowerCase();

            assertTrue("No response?", StringUtils.isNotBlank(body));
            EntityUtils.consume(entity);
            get.reset();
        } finally {
            server.stop();
        }
    }

}
