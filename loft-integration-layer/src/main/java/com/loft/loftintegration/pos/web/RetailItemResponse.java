package com.loft.loftintegration.pos.web;

import com.loft.loftintegration.pos.model.RetailItem;

import java.math.BigDecimal;

public record RetailItemResponse(Long id, String name, BigDecimal price, String currency) {
    public static RetailItemResponse from(RetailItem item) {
        return new RetailItemResponse(item.getId(), item.getName(), item.getPrice(), item.getCurrency());
    }
}
