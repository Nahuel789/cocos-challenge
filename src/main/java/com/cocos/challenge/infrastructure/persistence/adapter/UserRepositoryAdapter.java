package com.cocos.challenge.infrastructure.persistence.adapter;

import com.cocos.challenge.domain.exception.UserNotFoundException;
import com.cocos.challenge.domain.model.User;
import com.cocos.challenge.domain.repositories.UserRepository;
import com.cocos.challenge.infrastructure.persistence.mapper.Ids;
import com.cocos.challenge.infrastructure.persistence.mapper.PersistenceMapper;
import com.cocos.challenge.infrastructure.persistence.repository.SpringUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

	private final SpringUserRepository users;

	public UserRepositoryAdapter(SpringUserRepository users) {
		this.users = users;
	}

	@Override
	public Optional<User> findById(Long id) {
		return users.findById(Ids.db(id)).map(PersistenceMapper::toDomain);
	}

	@Override
	public void lockById(Long id) {
		if (users.lockById(Ids.db(id)).isEmpty()) {
			throw new UserNotFoundException(id);
		}
	}
}
