package com.loft.loftintegration.pos.web;

import com.loft.loftintegration.pos.model.PosSale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PosSaleResponse(
        Long id,
        String propertyCode,
        String guestProfileId,
        String currency,
        BigDecimal total,
        List<LineItemResponse> lineItems,
        LocalDateTime createdAt
) {
    public record LineItemResponse(String itemName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static PosSaleResponse from(PosSale sale) {
        return new PosSaleResponse(
                sale.getId(),
                sale.getPropertyCode(),
                sale.getGuestProfileId(),
                sale.getCurrency(),
                sale.total(),
                sale.getLineItems().stream()
                        .map(li -> new LineItemResponse(
                                li.getRetailItem().getName(), li.getQuantity(), li.getUnitPrice(), li.lineTotal()))
                        .toList(),
                sale.getCreatedAt()
        );
    }
}
