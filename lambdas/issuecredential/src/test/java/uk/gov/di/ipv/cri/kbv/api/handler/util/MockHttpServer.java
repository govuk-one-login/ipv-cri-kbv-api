package uk.gov.di.ipv.cri.kbv.api.handler.util;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class MockHttpServer {

    private static HttpServer server;

    public static void startServer(ArrayList<Injector> endpointList, int port) throws IOException {

        server = HttpServer.create(new InetSocketAddress(port), 0);
        endpointList.forEach(
                injector ->
                        server.createContext(
                                injector.getEndpoint(), new PreLambdaHandler(injector)));
        server.setExecutor(Executors.newCachedThreadPool()); // creates a default executor
        server.start();
    }

    public static void stopServer() {
        server.stop(0);
    }
}
