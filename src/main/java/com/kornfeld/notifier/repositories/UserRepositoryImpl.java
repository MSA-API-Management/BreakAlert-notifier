package com.kornfeld.notifier.repositories;

import com.kornfeld.notifier.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public abstract class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

}
