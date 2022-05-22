package com.izx.gateway.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString(callSuper = true)
@Builder
@Table("gateway_route")
public class GatewayRoute extends AbstractEntity {

    @Column( "route_id")
    private String routeId;

    @Column("uri")
    private String uri;

    @Column("ordered")
    private Integer ordered;

}
