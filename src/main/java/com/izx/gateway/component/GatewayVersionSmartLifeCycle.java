package com.izx.gateway.component;

import com.izx.gateway.util.DataConstant;
import com.izx.gateway.util.GatewayVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.retry.Repeat;

import java.time.Duration;

@Slf4j
@Component
public class GatewayVersionSmartLifeCycle implements SmartLifecycle {

    private boolean isRunning = false;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private ApplicationEventPublisher publisher;

    private Disposable disposable;

    @Override
    public void start() {
        /* 每10秒检查当前route version是否最新 不是最新时刷新
         * 监听 RefreshRoutesResultEvent 得到刷新结果
         * 刷新失败的原因大概率是因为路由规则设置错误 失败时重置版本为 0 不断重试
         */
        disposable =
                Mono.defer(() -> {
                            if (GatewayVersion.init.get()) {
                                return redisTemplate.opsForValue().get(DataConstant.REDIS_KEY_VERSION)
                                        .flatMap(v -> {
                                            Long version = Long.valueOf(v);
                                            if (version > GatewayVersion.version.get()) {
                                                //Gateway路由信息已更改,需要重新初始化
                                                GatewayVersion.version.set(version);
                                                this.publisher.publishEvent(new RefreshRoutesEvent(this));
                                            }
                                            return Mono.empty();
                                        });
                            }
                            return Mono.empty();
                        })
                        .repeatWhen(
                                Repeat.onlyIf(repeatContext -> true)
                                        .fixedBackoff(Duration.ofSeconds(10)))
                        .subscribeOn(Schedulers.boundedElastic()).subscribe();
        isRunning = true;
    }

    @Override
    public void stop() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

}
