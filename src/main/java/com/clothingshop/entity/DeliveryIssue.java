package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Delivery issue reported when a customer clicks "Chưa thấy hàng đâu" (table {@code DeliveryIssue}).
 * At most one issue per order.
 */
@Entity
@Table(name = "DeliveryIssue")
public class DeliveryIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryIssueStatus status = DeliveryIssueStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column
    private DeliveryResolution resolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvedBy")
    private Employee resolvedBy;

    @Column
    private LocalDateTime resolvedAt;

    protected DeliveryIssue() {
    }

    public Integer getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public DeliveryIssueStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryIssueStatus status) {
        this.status = status;
    }

    public DeliveryResolution getResolution() {
        return resolution;
    }

    public void setResolution(DeliveryResolution resolution) {
        this.resolution = resolution;
    }

    public Employee getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Employee resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
