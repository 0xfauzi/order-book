package com.oxfauzi.orderbook.command.order;

import com.oxfauzi.orderbook.util.BigDecimalUtils;
import com.oxfauzi.orderbook.util.DebugLogger;
import com.oxfauzi.orderbook.domain.Level;
import com.oxfauzi.orderbook.domain.Order;
import com.oxfauzi.orderbook.domain.OrderBookEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RemoveOrderCommand implements OrderBookCommand {

    private final TreeMap<BigDecimal, Level> buyLevels;
    private final TreeMap<BigDecimal, Level> sellLevels;
    private final Map<Order, OrderBookEntry> ordersEntries;

    public RemoveOrderCommand(TreeMap<BigDecimal, Level> buyLevels, TreeMap<BigDecimal, Level> sellLevels, Map<Order, OrderBookEntry> ordersEntries) {
        this.buyLevels = buyLevels;
        this.sellLevels = sellLevels;
        this.ordersEntries = ordersEntries;
    }

    @Override
    public void execute(Order order) {
        try {
            final OrderBookEntry orderBookEntry = ordersEntries.get(order);

            if (orderBookEntry.getPrevious() != null && orderBookEntry.getNext() != null) {
                orderBookEntry.getNext().setPrevious(orderBookEntry.getPrevious());
                orderBookEntry.getPrevious().setNext(orderBookEntry.getNext());
            } else if (orderBookEntry.getPrevious() != null) {
                orderBookEntry.getPrevious().setNext(null);
            } else if (orderBookEntry.getNext() != null) {
                orderBookEntry.getNext().setPrevious(null);
            }

            if (orderBookEntry.getLevel().getHead() == orderBookEntry && orderBookEntry.getLevel().getTail() == orderBookEntry) {
                orderBookEntry.getLevel().setHead(null);
                orderBookEntry.getLevel().setTail(null);
            } else if (orderBookEntry.getLevel().getHead() == orderBookEntry) {
                orderBookEntry.getLevel().setHead(orderBookEntry.getNext());
            } else if (orderBookEntry.getLevel().getTail() == orderBookEntry) {
                orderBookEntry.getLevel().setTail(orderBookEntry.getPrevious());
            }

            List<Order> orderEntriesToDelete = ordersEntries.keySet().stream()
                    .filter(oe -> BigDecimalUtils.equal(oe.getPrice(), order.getPrice()))
                    .toList();

            for (Order orderEntryToDelete : orderEntriesToDelete) {
                ordersEntries.remove(orderEntryToDelete);
                if (order.isBuy()) {
                    buyLevels.remove(orderEntryToDelete.getPrice());
                } else {
                    sellLevels.remove(orderEntryToDelete.getPrice());
                }
            }

            DebugLogger.log(" | REMOVE_ORDER | Order removed from order book: " + order);

        } catch (Exception e) {
            DebugLogger.log(" | REMOVE_ORDER | Exception: " + e.getMessage());
        }
    }
}
