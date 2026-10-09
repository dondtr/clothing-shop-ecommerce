package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key of {@link OrderDetail}: (orderId, productId).
 * Values are filled from the associations through {@code @MapsId}.
 */
@Embeddable
public class OrderDetailId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "productId")
    private Integer productId;

    protected OrderDetailId() {
    }

    public OrderDetailId(Integer orderId, Integer productId) {
        this.orderId = orderId;
        this.productId = productId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Integer getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderDetailId other)) {
            return false;
        }
        return Objects.equals(orderId, other.orderId) && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId);
    }
}
