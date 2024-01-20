package com.kornfeld.notifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DataChangeRepository extends JpaRepository<DataChange, Long> {
    Set<DataChange> findBySubscriptionId(Long id);
}
