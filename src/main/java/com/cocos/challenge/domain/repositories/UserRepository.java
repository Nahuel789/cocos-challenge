package com.cocos.challenge.domain.repositories;

import com.cocos.challenge.domain.model.User;

import java.util.Optional;

public interface UserRepository {

	Optional<User> findById(Long id);

	void lockById(Long id);
}
