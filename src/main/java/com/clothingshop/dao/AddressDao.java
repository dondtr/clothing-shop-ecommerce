package com.clothingshop.dao;

import com.clothingshop.entity.Address;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Data access for delivery addresses ({@code Address}, FR-06).
 */
public class AddressDao {

    private final EntityManager em;

    public AddressDao(EntityManager em) {
        this.em = em;
    }

    /** The customer's addresses, the default one first. */
    public List<Address> findByCustomer(int customerId) {
        return em.createQuery("select a from Address a where a.customer.id = :c order by a.defaultAddress desc, a.id", Address.class)
                .setParameter("c", customerId).getResultList();
    }

    /** The address when it exists and belongs to the customer. */
    public Optional<Address> findOwned(int customerId, int addressId) {
        return em.createQuery("select a from Address a where a.id = :id and a.customer.id = :c", Address.class)
                .setParameter("id", addressId).setParameter("c", customerId)
                .getResultStream().findFirst();
    }

    public void persist(Address address) {
        em.persist(address);
    }

    public void remove(Address address) {
        em.remove(address);
    }

    /** Writes pending changes now, for example a deletion that must happen before an insert. */
    public void flush() {
        em.flush();
    }
}
