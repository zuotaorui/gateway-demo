package com.izx.gateway.repository;

import com.izx.gateway.model.GatewayRouteArgs;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GatewayRouteArgsRepository extends R2dbcRepository<GatewayRouteArgs, Long> {

    Mono<GatewayRouteArgs> findByRouteIdAndTypeAndName(String routeId, String type, String name);

    Flux<GatewayRouteArgs> findByRouteId(String routeId);

    Mono<Void> deleteByRouteId(String routeId);
}
