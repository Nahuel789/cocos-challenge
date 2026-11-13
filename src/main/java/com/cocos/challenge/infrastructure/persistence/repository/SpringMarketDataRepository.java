package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SpringMarketDataRepository extends JpaRepository<MarketDataEntity, Integer> {

	@Query("""
			select m from MarketDataEntity m
			where m.instrumentId in :ids
			  and m.marketDate = (
			    select max(m2.marketDate) from MarketDataEntity m2
			    where m2.instrumentId = m.instrumentId
			  )
			""")
	List<MarketDataEntity> findLatest(@Param("ids") Collection<Integer> ids);
}
