# Proxy Server with Cache
This is a simple Java proxy server implementation that intercepts HTTP requests from clients, forwards them to remote servers, and caches the responses. The proxy server also supports blocking specific websites and setting expiration times for cached responses.

## Features
* HTTP Proxy Server: Listens for incoming HTTP requests from clients and forwards them to remote servers.
* Caching: Implements a Least Recently Used (LRU) cache to store and serve cached responses, reducing the number of requests to remote servers.
* Blocked Sites: Blocks access to specific websites by checking the requested host against a list of blocked sites.
* Request Validation: Validates incoming HTTP requests and handles various HTTP methods (GET, POST, PUT, DELETE).
* Error Handling: Provides error responses for unsupported features and internal server errors.
