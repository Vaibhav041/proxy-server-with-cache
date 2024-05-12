package dev.vaibhav;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCache {
    private final Node head;
    private final Node tail;
    private final int capacity;
    private final Map<String, Node> map;
    private final long expiration;

    public LRUCache(int capacity, long expiration) {
        this.capacity = capacity;
        this.expiration = expiration;
        head = new Node(null, null);
        tail = new Node(null, null);
        map = new ConcurrentHashMap<>();
        head.next = tail;
        tail.prev = head;
    }

    public synchronized String get(String address) {
        if (map.containsKey(address)) {
            Node node = map.get(address);
            if (isExpired(node)) {
                remove(node);
                return null;
            }
            remove(node);
            add(node);
            return node.data;
        }
        return null;
    }

    public synchronized void put(String address, String data) {
        if (map.containsKey(address)) {
            remove(map.get(address));
        }
        if (map.size() == capacity) {
            remove(tail.prev);
        }
        add(new Node(address, data));
    }

    private void add(Node node) {
        Node next = head.next;
        head.next = node;
        node.prev = head;
        node.next = next;
        next.prev = node;
        map.put(node.address, node);
    }

    private void remove(Node node) {
        Node prev = node.prev;
        Node next = node.next;
        prev.next = next;
        next.prev = prev;
        map.remove(node.address);
    }

    private boolean isExpired(Node node) {
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - node.createdTime;
        long expirationPeriod = expiration * 24 * 60 * 60 * 1000;
        return ageInMillis > expirationPeriod;
    }


    private class Node {
        private String address;
        private String data;
        private Node next;
        private Node prev;
        private long createdTime;

        private Node(String address, String data) {
            this.address = address;
            this.data = data;
            this.createdTime = System.currentTimeMillis();
        }
    }
}
