package com.izx.gateway.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.izx.gateway.model.GatewayRoute;
import com.izx.gateway.model.GatewayRouteArgs;
import com.izx.gateway.util.DataConstant;
import com.izx.gateway.util.GatewayVersion;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Repository
public class MySQLRouteDefinitionRepository implements RouteDefinitionRepository {

    @Autowired
    private GatewayRouteRepository gatewayRouteRepository;

    @Autowired
    private GatewayRouteArgsRepository gatewayRouteArgsRepository;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return initVersion().thenMany(findRouteDefinitions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(r -> {
            if (ObjectUtils.isEmpty(r.getId())) {
                return Mono.error(new IllegalArgumentException("id may not be empty"));
            } else {
                return saveGatewayRoute(r)
                        .thenMany(savePredicateGatewayRouteArgs(r))
                        .thenMany(saveFilterGatewayRouteArgs(r))
                        .then();
            }
        }).doOnSuccess(v -> updateVersion().subscribe());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> gatewayRouteRepository.findByRouteId(id)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("RouteDefinition not found: " + routeId)))
                .then(gatewayRouteRepository.deleteByRouteId(id))
                .then(gatewayRouteArgsRepository.deleteByRouteId(id))
        ).doOnSuccess(v -> updateVersion().subscribe());
    }

    private Mono<Void> initVersion() {
        if (GatewayVersion.init.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            return redisTemplate.opsForValue().get(DataConstant.REDIS_KEY_VERSION)
                    .switchIfEmpty(Mono.just(String.valueOf(Instant.now().toEpochMilli())))
                    .flatMap(version -> {
                        GatewayVersion.version.set(Long.valueOf(version));
                        return Mono.empty();
                    });
        }
        return Mono.empty();
    }

    private Mono<Boolean> updateVersion() {
        Long timestamp = Instant.now().toEpochMilli();
        return redisTemplate.opsForValue().get(DataConstant.REDIS_KEY_VERSION)
                .switchIfEmpty(Mono.just(StringUtils.EMPTY))
                .flatMap(version -> {
                    //说明其他Gateway实例再次更新了routes 将当前实例的route version重置为0
                    if (StringUtils.isNotBlank(version) && Long.valueOf(version) > timestamp) {
                        GatewayVersion.version.set(0L);
                        return Mono.empty();
                    } else {
                       return redisTemplate.opsForValue().set(DataConstant.REDIS_KEY_VERSION,String.valueOf(timestamp));
                    }
                });
    }

    private Flux<RouteDefinition> findRouteDefinitions() {
        return gatewayRouteRepository.findAll()
                .publishOn(Schedulers.boundedElastic())
                .map(gatewayRoute -> {
                    RouteDefinition routeDefinition = new RouteDefinition();
                    routeDefinition.setId(gatewayRoute.getRouteId());
                    URI uri;
                    if (StringUtils.isBlank(gatewayRoute.getUri())) {
                        uri = null;
                    } else {
                        uri = URI.create(gatewayRoute.getUri());
                    }
                    routeDefinition.setUri(uri);
                    routeDefinition.setOrder(gatewayRoute.getOrdered());
                    return routeDefinition;
                })
                .flatMap(rd -> gatewayRouteArgsRepository.findByRouteId(rd.getId())
                        .collectList()
                        .map(list -> {
                            for (GatewayRouteArgs gatewayRouteArgs : list) {
                                if (DataConstant.STR_PREDICATE.equalsIgnoreCase(
                                        gatewayRouteArgs.getType())) {
                                    PredicateDefinition pd = new PredicateDefinition();
                                    pd.setName(gatewayRouteArgs.getName());
                                    pd.setArgs(JSONObject.parseObject(gatewayRouteArgs.getArgs(),
                                            Map.class));
                                    rd.getPredicates().add(pd);
                                } else if (DataConstant.STR_FILTER.equalsIgnoreCase(
                                        gatewayRouteArgs.getType())) {
                                    FilterDefinition fd = new FilterDefinition();
                                    fd.setName(gatewayRouteArgs.getName());
                                    fd.setArgs(JSONObject.parseObject(gatewayRouteArgs.getArgs(),
                                            Map.class));
                                    rd.getFilters().add(fd);
                                }
                            }
                            return rd;
                        })
                );
    }

    private Mono<GatewayRoute> saveGatewayRoute(RouteDefinition rd) {
        return gatewayRouteRepository.findByRouteId(rd.getId())
                .switchIfEmpty(Mono.just(GatewayRoute.builder().routeId(rd.getId()).build()))
                .flatMap(gatewayRoute -> {
                    String uriStr = StringUtils.EMPTY;
                    if (rd.getUri() != null) {
                        uriStr = rd.getUri().toString();
                    }
                    gatewayRoute.setUri(uriStr);
                    gatewayRoute.setOrdered(rd.getOrder());
                    return gatewayRouteRepository.save(gatewayRoute);
                });
    }

    private Flux<GatewayRouteArgs> savePredicateGatewayRouteArgs(RouteDefinition rd) {
        return Flux.fromIterable(rd.getPredicates())
                .flatMap(pd -> gatewayRouteArgsRepository.findByRouteIdAndTypeAndName(
                                rd.getId(), DataConstant.STR_PREDICATE, pd.getName())
                        .switchIfEmpty(Mono.just(GatewayRouteArgs.builder().routeId(rd.getId())
                                .type(DataConstant.STR_PREDICATE)
                                .name(pd.getName())
                                .build()))
                        .flatMap(gatewayRouteArgs -> {
                            JSONObject args = JSONObject.parseObject(
                                    gatewayRouteArgs.getArgs());
                            if (args == null) {
                                args = new JSONObject();
                            }
                            args.putAll(pd.getArgs());
                            gatewayRouteArgs.setArgs(args.toJSONString());
                            return gatewayRouteArgsRepository.save(
                                    gatewayRouteArgs);
                        }));
    }

    private Flux<GatewayRouteArgs> saveFilterGatewayRouteArgs(RouteDefinition rd) {
        return Flux.fromIterable(rd.getFilters())
                .flatMap(fd -> gatewayRouteArgsRepository.findByRouteIdAndTypeAndName(
                                rd.getId(), DataConstant.STR_FILTER, fd.getName())
                        .switchIfEmpty(Mono.just(GatewayRouteArgs.builder().routeId(rd.getId())
                                .type(DataConstant.STR_FILTER)
                                .name(fd.getName())
                                .build()))
                        .flatMap(gatewayRouteArgs -> {
                            JSONObject args = new JSONObject();
                            if (JSON.isValid(gatewayRouteArgs.getArgs())) {
                                args = JSONObject.parseObject(gatewayRouteArgs.getArgs());
                            }
                            args.putAll(fd.getArgs());
                            gatewayRouteArgs.setArgs(args.toJSONString());
                            return gatewayRouteArgsRepository.save(gatewayRouteArgs);
                        }));
    }

}
