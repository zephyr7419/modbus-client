package com.example.ModbusClient.util.modbus;

import java.util.LinkedList;
import java.util.Queue;

public class RequestQueue {
    private Queue<Request> queue = new LinkedList();

    public void addRequest(Request request) {
        queue.add(request);
    }

    public Request getNextRequest() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
