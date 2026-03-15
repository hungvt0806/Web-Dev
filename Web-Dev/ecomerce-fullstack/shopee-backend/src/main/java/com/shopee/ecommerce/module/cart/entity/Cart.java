package com.shopee.ecommerce.module.cart.entity;

import com.shopee.ecommerce.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "session_id", unique = true, length = 128)
    private String sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}