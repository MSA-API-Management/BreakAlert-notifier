package com.kornfeld.notifier.repositories;

import com.kornfeld.notifier.entities.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Subscription save(Subscription subscription);

    Optional<Subscription> findById(Long id);

    Optional<Subscription> findByAddressAndBranch(String address, String branch);
}
