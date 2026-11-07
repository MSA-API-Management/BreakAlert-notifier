package net.jamnig.breakalert.repositories;

import net.jamnig.breakalert.entities.DataChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface DataChangeRepository extends JpaRepository<DataChange, Long> {
    Set<DataChange> findBySubscriptionId(Long id);

    DataChange save(DataChange dataChange);
}
