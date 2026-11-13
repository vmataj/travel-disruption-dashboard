package com.vmataj.travel;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

interface PlannerTripRepository extends JpaRepository<PlannerTrip, UUID> {
    @Query("select t from PlannerTrip t where t.owner = :user or :user member of t.collaborators order by t.startDate, t.id")
    List<PlannerTrip> visibleTo(String user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PlannerTrip t where t.id = :id")
    Optional<PlannerTrip> lock(UUID id);
}

interface SavedDestinationRepository extends JpaRepository<SavedDestination, String> {
    List<SavedDestination> findByOwnerOrderByDestinationName(String owner);
}
