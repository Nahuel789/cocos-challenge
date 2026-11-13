package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringOrderRepository extends JpaRepository<OrderEntity, Integer> {

	List<OrderEntity> findByUserIdOrderByDatetimeDescIdDesc(Integer userId);

	Page<OrderEntity> findByUserIdOrderByDatetimeDescIdDesc(Integer userId, Pageable pageable);
}
