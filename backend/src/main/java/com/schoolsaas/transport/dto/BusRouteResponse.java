package com.schoolsaas.transport.dto;

import com.schoolsaas.transport.BusRoute;

public record BusRouteResponse(Long id, String label) {

    public static BusRouteResponse from(BusRoute route) {
        return new BusRouteResponse(route.getId(), route.getLabel());
    }
}
