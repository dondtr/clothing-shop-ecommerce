package com.clothingshop.dao;

import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Data access for employee accounts ({@code Employee}, a subtype of {@code User}).
 */
public class EmployeeDao {

    private final EntityManager em;

    public EmployeeDao(EntityManager em) {
        this.em = em;
    }

    /** Every employee, the manager first, then the most recently hired. */
    public List<Employee> findAll() {
        return em.createQuery("select e from Employee e order by e.role desc, e.hireDate desc, e.id desc", Employee.class)
                .getResultList();
    }

    public Optional<Employee> findById(int id) {
        return Optional.ofNullable(em.find(Employee.class, id));
    }

    /** A reference to the employee without loading it, for "handled by" / "resolved by" columns. */
    public Employee reference(int id) {
        return em.getReference(Employee.class, id);
    }

    public boolean existsWithRole(EmployeeRole role) {
        return em.createQuery("select count(e) from Employee e where e.role = :r", Long.class)
                .setParameter("r", role).getSingleResult() > 0;
    }

    public void persist(Employee employee) {
        em.persist(employee);
    }
}
