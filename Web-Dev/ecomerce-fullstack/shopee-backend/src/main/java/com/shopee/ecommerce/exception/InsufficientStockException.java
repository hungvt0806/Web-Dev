package com.shopee.ecommerce.exception;

/**
 * Thrown when requested quantity exceeds available stock for a product variant.
 *
 * Example:
 *   Variant SKU-001 has only 3 units in stock, but 5 were requested.
 */
public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final int    available;
    private final int    requested;

    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for SKU '" + sku + "': "
                + "requested=" + requested + ", available=" + available);
        this.sku       = sku;
        this.available = available;
        this.requested = requested;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.sku       = null;
        this.available = 0;
        this.requested = 0;
    }

    public String getSku()       { return sku; }
    public int    getAvailable() { return available; }
    public int    getRequested() { return requested; }
}