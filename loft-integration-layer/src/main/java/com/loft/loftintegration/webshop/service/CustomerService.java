package com.loft.loftintegration.webshop.service;

import com.loft.loftintegration.webshop.model.Customer;
import com.loft.loftintegration.webshop.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Looks up an existing Customer by email, or creates one. This is
     * the entire "identity" model for the webshop — no password, no
     * session. Booking again with the same email reuses the same
     * Customer row; name/phone on the existing row are NOT overwritten
     * by a later booking with different values (e.g. someone booking
     * for a friend using their own email but the friend's name) — that
     * ambiguity is a real product question for later, not silently
     * guessed at here.
     */
    @Transactional
    public Customer findOrCreate(String name, String email, String phone) {
        return customerRepository.findByEmail(email)
                .orElseGet(() -> customerRepository.save(new Customer(name, email, phone)));
    }
}
