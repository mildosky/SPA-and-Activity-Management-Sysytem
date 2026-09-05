package com.loft.loftintegration.webshop.model;

import jakarta.persistence.*;

/**
 * A webshop customer — deliberately NOT an authenticated account. No
 * password, no login session. Identified purely by email: booking
 * again with the same email reuses the same Customer row rather than
 * creating a duplicate (see CustomerService.findOrCreate()).
 *
 * This is a real product decision, not a shortcut: most guests booking
 * a spa treatment or buying a gift certificate don't want to create an
 * account first. If "my bookings" history is wanted later, the
 * lightweight path is an emailed magic link looked up against this
 * email, not full auth — email is already the unique identity guests
 * naturally have.
 */
@Entity
@Table(name = "webshop_customer", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String phone;

    protected Customer() {
        // JPA
    }

    public Customer(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
