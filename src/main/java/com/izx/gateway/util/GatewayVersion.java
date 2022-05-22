package com.izx.gateway.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class GatewayVersion {

    public  static final AtomicLong version=new AtomicLong(0);

    public static final AtomicBoolean init= new AtomicBoolean(false);
}
