package com.izx.gateway.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@ToString(callSuper = true)
@Table("gateway_route_args")
public class GatewayRouteArgs extends AbstractEntity {

    @Column("route_id")
    private String routeId;

    @Column("type")
    private String type;

    @Column("name")
    private String name;

    @Column("args")
    private String args;

}
