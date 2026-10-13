package com.clothingshop.service;

import com.clothingshop.dao.CustomerDao;
import com.clothingshop.dao.DeliveryIssueDao;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.ReturnRequestDao;
import com.clothingshop.dto.OrderRow;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.util.JpaUtil;
import java.util.List;
import java.util.Map;

/** Figures for the first page of the staff area: work waiting for employees and the latest orders. */
public class DashboardService {

    /**
     * @param orderCounts    visible orders per status (orders waiting for a VNPAY payment excluded, BR-26)
     * @param openIssues     "Chưa thấy hàng đâu" reports of orders still being delivered, waiting for staff (FR-21)
     * @param pendingReturns return requests waiting for a decision (FR-26)
     */
    public record Dashboard(Map<OrderStatus, Long> orderCounts, long pendingOrders, long confirmedOrders, long openIssues,
                            long pendingReturns, long customers, long productsOnSale, List<OrderRow> latestOrders) {
    }

    public Dashboard load() {
        return JpaUtil.read(em -> {
            OrderDao orders = new OrderDao(em);
            long openIssues = new DeliveryIssueDao(em).countOpenOfShippingOrders();
            long pendingReturns = new ReturnRequestDao(em).countPending();
            long customers = new CustomerDao(em).count();
            long products = new ProductDao(em).countProductsOnSale();
            Map<OrderStatus, Long> counts = orders.countVisibleByStatus();
            return new Dashboard(counts, counts.get(OrderStatus.PENDING), counts.get(OrderStatus.CONFIRMED), openIssues,
                    pendingReturns, customers, products, orders.findVisible(null, 10));
        });
    }
}
