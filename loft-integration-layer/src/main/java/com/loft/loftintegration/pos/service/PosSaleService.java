package com.loft.loftintegration.pos.service;

import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.model.PosSale;
import com.loft.loftintegration.pos.model.PosSaleLineItem;
import com.loft.loftintegration.pos.model.RetailItem;
import com.loft.loftintegration.pos.repository.PosSaleRepository;
import com.loft.loftintegration.pos.repository.RetailItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class PosSaleService {

    private final RetailItemRepository retailItemRepository;
    private final PosSaleRepository posSaleRepository;
    private final BillingService billingService;

    public PosSaleService(RetailItemRepository retailItemRepository, PosSaleRepository posSaleRepository,
                           BillingService billingService) {
        this.retailItemRepository = retailItemRepository;
        this.posSaleRepository = posSaleRepository;
        this.billingService = billingService;
    }

    /**
     * Creates a sale from a map of retailItemId -> quantity, prices each
     * line at the RetailItem's CURRENT price (captured into the line
     * item so later price changes don't retroactively alter this sale),
     * and immediately generates a Charge for the total via
     * BillingService — a retail sale is complete and billable the
     * moment it's rung up, unlike a Booking which goes through its own
     * PENDING_PAYMENT/CONFIRMED lifecycle first.
     */
    @Transactional
    public PosSale createSale(String propertyCode, String guestProfileId, String operaReservationId,
                               String currency, Map<Long, Integer> itemQuantities) {
        if (itemQuantities.isEmpty()) {
            throw new IllegalArgumentException("A sale needs at least one line item.");
        }

        PosSale sale = new PosSale(propertyCode, guestProfileId, operaReservationId, currency);

        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            RetailItem item = retailItemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSuchElementException("No RetailItem with id " + entry.getKey()));
            sale.addLineItem(new PosSaleLineItem(item, entry.getValue(), item.getPrice()));
        }

        sale = posSaleRepository.save(sale);
        billingService.chargeForPosSale(sale);
        return sale;
    }

    public List<RetailItem> listActiveItems(String propertyCode) {
        return retailItemRepository.findByPropertyCodeAndActiveTrue(propertyCode);
    }
}
