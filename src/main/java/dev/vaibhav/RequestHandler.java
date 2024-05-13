package dev.vaibhav;

import dev.vaibhav.exception.BlockedSiteException;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestHandler {
    private final Socket clientSocket;
    private final LRUCache cache;
    private final Set<String> blockedSites;

    public RequestHandler(Socket clientSocket, LRUCache cache) {
        this.clientSocket = clientSocket;
        this.cache = cache;
        this.blockedSites = loadBlockedSitesFromFile("blocked_sites.txt");
    }

    public void serve() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (isAllowedHeader(line)) {
                    requestBuilder.append(line).append("\r\n");
                    if (line.startsWith("Host")) {
                        requestBuilder.append("Connection: close\r\n");
                    }
                }
            }
            requestBuilder.append("\r\n");
            int contentLength = getContentLength(requestBuilder.toString());
            if (contentLength > 0) {
                char[] bodyBuffer = new char[contentLength];
                in.read(bodyBuffer, 0, contentLength);
                requestBuilder.append(bodyBuffer);
            }
            System.out.println("request from client: " + requestBuilder);

            validateRequest(requestBuilder.toString());

            String response = handleRemoteRequest(requestBuilder.toString());
            out.println(response);
            out.close();
            in.close();
            clientSocket.close();
            System.out.println("client request served successfully: " + clientSocket.getInetAddress());
        } catch (Exception e) {
            closeConnection();
            System.out.println("client request failed: " + clientSocket.getInetAddress());
        }
    }

    private String handleRemoteRequest(String request) throws Exception {
        String address = getAddressFromRequest(request);
        URL url = new URL(address);

        if (isSiteBlocked(url.getHost())) {
            throw new BlockedSiteException("Site is blocked");
        }

        String cachedResponse = cache.get(address);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try(
                Socket remoteSocket = new Socket(url.getHost(), url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
                PrintWriter out = new PrintWriter(remoteSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(remoteSocket.getInputStream()));
        ) {
            remoteSocket.setSoTimeout(10000);
            out.write(request);
            out.flush();
            StringBuilder responseBuilder = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine).append("\n");
            }

            cache.put(address, responseBuilder.toString());

            return responseBuilder.toString();
        }  catch (UnknownHostException e) {
            handleErrorResponse(400, "Host not found");
            throw e;
        } catch (BlockedSiteException e) {
            handleErrorResponse(403, e.getMessage());
            throw e;
        } catch (Exception e) {
            handleErrorResponse(500, e.getMessage());
            throw e;
        }
    }

    private String getAddressFromRequest(String request) {
        String[] requestParts = request.split("\r\n");
        return requestParts[0].split(" ")[1];
    }

    private int getContentLength(String request) {
        String[] lines = request.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("Content-Length:")) {
                return Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }
        return 0;
    }

    private void handleErrorResponse(int statusCode, String message) {
        try(PrintWriter  out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            sendErrorResponse(out, statusCode, message);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendErrorResponse(PrintWriter out, int statusCode, String message) {
        out.println("HTTP/1.1 " + statusCode + " " + getHttpStatusMessage(statusCode));
        out.println("Content-Type: text/plain");
        out.println();
        out.println(message);
        out.flush();
    }

    private String getHttpStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown";
        };
    }


    private boolean isAllowedHeader(String header) {
        for (String allowedHeader : ALLOWED_HEADERS) {
            if (header.startsWith(allowedHeader)) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> ALLOWED_HEADERS = List.of("GET", "POST", "PUT", "DELETE", "Host", "Content-Type", "Content-Length");

    private void closeConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSiteBlocked(String host) {
        return blockedSites.contains(host);
    }

    private void validateRequest(String request) throws Exception {
        String[] requestParts = request.split("\r\n");
        String r = requestParts[0];

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        if (r.startsWith("CONNECT")) {
            sendErrorResponse(out, 503, "Not implemented");
            throw new RuntimeException("Not implemented");
        }

        if (!r.startsWith("GET") && !r.startsWith("POST") && !r.startsWith("PUT") && !r.startsWith("DELETE")) {
            sendErrorResponse(out, 400, "Bad request");
            throw new RuntimeException("Bad request");
        }

        out.close();
    }

    private Set<String> loadBlockedSitesFromFile(String fileName) {
        Set<String> blockedSitesSet = new HashSet<>();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    blockedSitesSet.add(line);
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to load blocked sites from file: " + fileName);
        }
        return blockedSitesSet;
    }
}
