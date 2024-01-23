package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public abstract class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

}
