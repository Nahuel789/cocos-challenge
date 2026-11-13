package com.cocos.challenge.infrastructure.persistence.adapter;

import com.cocos.challenge.domain.events.OrderEventPublisher;
import com.cocos.challenge.domain.exception.OrderNotFoundException;
import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;
import com.cocos.challenge.domain.repositories.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.mapper.Ids;
import com.cocos.challenge.infrastructure.persistence.mapper.Pages;
import com.cocos.challenge.infrastructure.persistence.mapper.PersistenceMapper;
import com.cocos.challenge.infrastructure.persistence.repository.SpringOrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

@Log4j2
@Repository
public class OrderRepositoryAdapter implements OrderRepository {

	private final SpringOrderRepository orders;
	private final OrderEventPublisher events;

	public OrderRepositoryAdapter(SpringOrderRepository orders, OrderEventPublisher events) {
		this.orders = orders;
		this.events = events;
	}

	@Override
	public Order save(Order order) {
		Order saved;
		if (order.id() == null) {
			saved = PersistenceMapper.toDomain(orders.save(PersistenceMapper.toNewEntity(order)));
		} else {
			OrderEntity entity = orders.findById(Ids.db(order.id())).orElseThrow(() -> new OrderNotFoundException(order.id()));
			PersistenceMapper.copy(order, entity);
			saved = PersistenceMapper.toDomain(orders.save(entity));
		}
		log.info("Order saved successfully id={}, userId={}, instrumentId={}, status={}",
				saved.id(), saved.userId(), saved.instrumentId(), saved.status());
		publishAfterCommit(saved);
		return saved;
	}

	private void publishAfterCommit(Order saved) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					events.publish(saved);
				}
			});
			return;
		}
		events.publish(saved);
	}

	@Override
	public Optional<Order> findById(Long id) {
		return orders.findById(Ids.db(id)).map(PersistenceMapper::toDomain);
	}

	@Override
	public List<Order> findByUserId(Long userId) {
		return orders.findByUserIdOrderByDatetimeDescIdDesc(Ids.db(userId)).stream()
				.map(PersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Page<Order> findByUserId(Long userId, PageQuery pageQuery) {
		return Pages.domain(
				orders.findByUserIdOrderByDatetimeDescIdDesc(Ids.db(userId), Pages.db(pageQuery)),
				PersistenceMapper::toDomain);
	}
}
