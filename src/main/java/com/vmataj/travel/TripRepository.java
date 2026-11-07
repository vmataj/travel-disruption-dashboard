package com.vmataj.travel;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TripRepository extends JpaRepository<Trip, String> {
    @Query("""
        select t from Trip t
        where (:route is null or t.route = :route)
          and (:departure is null or t.departure = :departure)
          and (:status is null or t.status = :status)
        order by t.departure, t.code
        """)
    List<Trip> search(String route, LocalDate departure, Trip.Status status);

    @Query("select distinct t.route from Trip t order by t.route")
    List<String> routes();
}
