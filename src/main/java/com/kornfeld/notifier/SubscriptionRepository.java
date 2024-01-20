package com.kornfeld.notifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Subscription save(Subscription subscription);
}
