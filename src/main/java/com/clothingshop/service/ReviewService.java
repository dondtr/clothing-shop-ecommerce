package com.clothingshop.service;

import com.clothingshop.entity.Customer;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Review;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Product reviews (FR-29, FR-30, BR-14): a customer reviews a product bought in a completed order, once per product
 * (rows sharing a name, Q44), and may delete their own reviews. Reviews stay when a product stops being sold.
 */
public class ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewService.class);

    /** Review.comment is TEXT; a limit keeps comments reasonable. */
    static final int COMMENT_MAX = 2000;

    private final Clock clock;

    public ReviewService(Clock clock) {
        this.clock = clock;
    }

    /**
     * FR-29 (Review.create()): {@code productId} is the row the customer bought, found in one of their COMPLETED orders.
     *
     * @return the new review's id
     */
    public int create(int customerId, int productId, String ratingText, String comment) {
        Map<String, String> errors = new LinkedHashMap<>();
        int rating = 0;
        try {
            rating = Integer.parseInt(ratingText == null ? "" : ratingText.trim());
        } catch (NumberFormatException e) {
            rating = 0;
        }
        if (rating < 1 || rating > 5) {   // Review.isValidRating()
            errors.put("rating", "Vui lòng chọn từ 1 đến 5 sao");
        }
        String text = comment == null || comment.isBlank() ? null : comment.trim();
        if (text != null && text.length() > COMMENT_MAX) {
            errors.put("comment", "Bình luận tối đa " + COMMENT_MAX + " ký tự");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        int stars = rating;
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            int id = JpaUtil.inTransaction(em -> {
                // One review per customer and product: reviews of one customer are created one at a time
                Customer customer = em.find(Customer.class, customerId, LockModeType.PESSIMISTIC_WRITE);
                Product product = em.find(Product.class, productId);
                if (customer == null || product == null) {
                    throw new BusinessException("Sản phẩm không tồn tại");
                }
                boolean bought = em.createQuery("select count(d) from OrderDetail d where d.order.customer.id = :c"
                                + " and d.order.status = :completed and d.product.id = :p", Long.class)
                        .setParameter("c", customerId).setParameter("completed", OrderStatus.COMPLETED)
                        .setParameter("p", productId).getSingleResult() > 0;
                if (!bought) {
                    throw new BusinessException("Chỉ đánh giá được sản phẩm đã mua trong đơn giao thành công (BR-14)");
                }
                boolean reviewed = em.createQuery("select count(r) from Review r where r.customer.id = :c"
                                + " and lower(r.product.name) = lower(:name)", Long.class)
                        .setParameter("c", customerId).setParameter("name", product.getName()).getSingleResult() > 0;
                if (reviewed) {
                    throw new BusinessException("Bạn đã đánh giá sản phẩm này");
                }
                Review review = new Review(customer, product, stars, text, now);
                em.persist(review);
                return review.getId();
            });
            LOG.info("Review created: id={}, customerId={}, productId={}", id, customerId, productId);
            return id;
        } catch (PersistenceException e) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này");
        }
    }

    /** FR-30 (Review.delete()): only the customer's own review. */
    public void delete(int customerId, int reviewId) {
        JpaUtil.inTransaction(em -> {
            Review review = em.find(Review.class, reviewId);
            if (review == null || review.getCustomer().getId() != customerId) {
                throw new BusinessException("Đánh giá không tồn tại");
            }
            em.remove(review);
            return null;
        });
        LOG.info("Review deleted: id={}, customerId={}", reviewId, customerId);
    }
}
