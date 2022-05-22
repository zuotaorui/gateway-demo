package com.izx.gateway.repository;

import com.izx.gateway.model.GatewayRoute;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface GatewayRouteRepository extends ReactiveCrudRepository<GatewayRoute,Long> {

    Mono<GatewayRoute> findByRouteId(String routeId);

    Mono<Void> deleteByRouteId(String routeId);
}
