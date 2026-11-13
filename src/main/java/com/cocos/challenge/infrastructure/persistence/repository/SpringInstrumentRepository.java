package com.cocos.challenge.infrastructure.persistence.repository;

import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringInstrumentRepository extends JpaRepository<InstrumentEntity, Integer> {

	@Query(value = """
			select i from InstrumentEntity i
			where (cast(:ticker as string) is null or lower(i.ticker) like lower(concat('%', cast(:ticker as string), '%')) escape '\\')
			  and (cast(:name as string) is null or lower(i.name) like lower(concat('%', cast(:name as string), '%')) escape '\\')
			order by i.ticker asc, i.id asc
			""",
			countQuery = """
			select count(i) from InstrumentEntity i
			where (cast(:ticker as string) is null or lower(i.ticker) like lower(concat('%', cast(:ticker as string), '%')) escape '\\')
			  and (cast(:name as string) is null or lower(i.name) like lower(concat('%', cast(:name as string), '%')) escape '\\')
			""")
	Page<InstrumentEntity> search(@Param("ticker") String ticker, @Param("name") String name, Pageable pageable);
}
