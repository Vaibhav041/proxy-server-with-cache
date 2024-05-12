package dev.vaibhav;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
    private static final int PROXY_PORT = 8080;
    private static final int NUMBER_OF_THREADS = 10;
    private static final int CACHE_SIZE = 100;
    private static final long CACHE_EXPIRATION = 20L;
    private ProxyServer() {}

    public static void main(String[] args) {
        try(
                ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
                ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)
        ) {
            System.out.println("Server started on port: " + serverSocket.getLocalPort());

            LRUCache cache = new LRUCache(CACHE_SIZE, CACHE_EXPIRATION);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                executorService.execute(() -> new RequestHandler(clientSocket, cache).serve());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}