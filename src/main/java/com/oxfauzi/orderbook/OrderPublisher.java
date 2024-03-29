package com.oxfauzi.orderbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxfauzi.orderbook.domain.Order;
import com.oxfauzi.orderbook.domain.OrderRequest;
import com.oxfauzi.orderbook.util.DebugLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import static com.oxfauzi.orderbook.domain.Constants.POISON_ORDER;

public class OrderPublisher {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch messageLatch;
    private final StringBuilder completeMessageBuilder;
    private final BlockingQueue<Order> orderQueue;

    public OrderPublisher(CountDownLatch messageLatch,
                          BlockingQueue<Order> orderQueue,
                          StringBuilder completeMessageBuilder) {
        this.messageLatch = messageLatch;
        this.orderQueue = orderQueue;
        this.completeMessageBuilder = completeMessageBuilder;
    }

    public void publishOrder(String message, boolean last) {
        try {
            completeMessageBuilder.append(message);
            if (last) {
                String completeMessage = completeMessageBuilder.toString();
                if (completeMessage.contains("l2update")) {
                    OrderRequest orderRequest = objectMapper.readValue(completeMessage, OrderRequest.class);
                    System.out.println(orderRequest);
                    orderRequest.getChanges().forEach(order -> {
                        try {
                            System.out.println(order);
                            orderQueue.put(order);
                        } catch (InterruptedException e) {
                            System.err.println("Exception: " + e);
                            sendPoisonMessage();
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                completeMessageBuilder.setLength(0); //reset since we've completed a message
            }
        } catch (Exception e) {
            DebugLogger.log(" | MAIN | Exception: " + e.getMessage());
            sendPoisonMessage();
        }
    }

    private void sendPoisonMessage() {
        try {
            messageLatch.countDown();
            orderQueue.put(POISON_ORDER);
        } catch (InterruptedException ex) {
            DebugLogger.log(" | MAIN | Interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
