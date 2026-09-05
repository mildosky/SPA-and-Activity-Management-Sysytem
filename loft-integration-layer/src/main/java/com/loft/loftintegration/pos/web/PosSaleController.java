package com.loft.loftintegration.pos.web;

import com.loft.loftintegration.pos.model.PosSale;
import com.loft.loftintegration.pos.service.PosSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pos")
public class PosSaleController {

    private final PosSaleService posSaleService;

    public PosSaleController(PosSaleService posSaleService) {
        this.posSaleService = posSaleService;
    }

    @GetMapping("/retail-items")
    public java.util.List<RetailItemResponse> listRetailItems(@RequestParam("propertyCode") String propertyCode) {
        return posSaleService.listActiveItems(propertyCode).stream()
                .map(RetailItemResponse::from)
                .toList();
    }

    @PostMapping("/sales")
    public PosSaleResponse createSale(@RequestBody CreatePosSaleRequest request) {
        try {
            PosSale sale = posSaleService.createSale(
                    request.propertyCode(), request.guestProfileId(), request.operaReservationId(),
                    request.currency(), request.itemQuantities());
            return PosSaleResponse.from(sale);
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
