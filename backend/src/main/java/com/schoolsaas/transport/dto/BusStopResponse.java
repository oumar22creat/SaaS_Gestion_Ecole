package com.schoolsaas.transport.dto;

import com.schoolsaas.transport.BusStop;

public record BusStopResponse(Long id, Long busRouteId, String name, int sequenceOrder) {

    public static BusStopResponse from(BusStop stop) {
        return new BusStopResponse(stop.getId(), stop.getBusRouteId(), stop.getName(), stop.getSequenceOrder());
    }
}
