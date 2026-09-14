package com.schoolsaas.transport;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusStopRepository extends JpaRepository<BusStop, Long> {

    List<BusStop> findAllByBusRouteIdOrderBySequenceOrderAsc(Long busRouteId);
}
