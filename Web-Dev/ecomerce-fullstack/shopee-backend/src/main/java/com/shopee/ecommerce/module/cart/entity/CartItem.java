package com.shopee.ecommerce.module.cart.entity;
import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="cart_items") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CartItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cart_id",nullable=false) private Cart cart;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private Product product;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id") private ProductVariant variant;
    @Column(name="quantity",nullable=false) private int quantity;
    @Column(name="unit_price",nullable=false,precision=15,scale=2) private BigDecimal unitPrice;
}
