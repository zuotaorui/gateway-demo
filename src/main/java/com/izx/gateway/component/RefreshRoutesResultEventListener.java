package com.izx.gateway.component;

import com.izx.gateway.util.GatewayVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefreshRoutesResultEventListener
        implements ApplicationListener<RefreshRoutesResultEvent> {

    @Override
    public void onApplicationEvent(RefreshRoutesResultEvent event) {
        if (event.isSuccess()) {
            log.info("RefreshRoutesResultEventListener | refresh routes success. event:{}", event);
        } else {
            GatewayVersion.version.set(0);
            log.error("RefreshRoutesResultEventListener | refresh routes failed. event:{} e:{}",
                    event, event.getThrowable());
        }
    }
}
