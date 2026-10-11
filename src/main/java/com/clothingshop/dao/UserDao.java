package com.clothingshop.dao;

import com.clothingshop.entity.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * Data access for accounts ({@code User} and its subtypes {@code Customer}, {@code Employee}).
 *
 * <p>User names and emails are compared without regard to letter case (BR-17). {@code lower()} keeps the comparison
 * correct on any database; with the collation utf8mb4_0900_as_ci MySQL would also compare case-insensitively.
 */
public class UserDao {

    private final EntityManager em;

    public UserDao(EntityManager em) {
        this.em = em;
    }

    public boolean existsByUserName(String userName) {
        return em.createQuery("select count(u) from User u where lower(u.userName) = lower(:v)", Long.class)
                .setParameter("v", userName).getSingleResult() > 0;
    }

    public boolean existsByEmail(String email) {
        return em.createQuery("select count(u) from User u where lower(u.email) = lower(:v)", Long.class)
                .setParameter("v", email).getSingleResult() > 0;
    }

    public Optional<User> findByUserName(String userName) {
        return em.createQuery("select u from User u where lower(u.userName) = lower(:v)", User.class)
                .setParameter("v", userName).getResultStream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return em.createQuery("select u from User u where lower(u.email) = lower(:v)", User.class)
                .setParameter("v", email).getResultStream().findFirst();
    }

    public void persist(User user) {
        em.persist(user);
    }
}
