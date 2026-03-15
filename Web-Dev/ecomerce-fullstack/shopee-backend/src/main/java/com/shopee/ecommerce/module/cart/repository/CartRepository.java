package com.shopee.ecommerce.module.cart.repository;
import com.shopee.ecommerce.module.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional; import java.util.UUID;
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.variant WHERE c.user.id=:userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") UUID userId);
    Optional<Cart> findByUserId(UUID userId);
}
