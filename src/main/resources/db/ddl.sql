-- gateway.gateway_route definition

CREATE TABLE `gateway_route` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `route_id` varchar(100) NOT NULL,
     `uri` varchar(200) NOT NULL,
     `ordered` int(11) DEFAULT '0',
     `created_at` datetime NOT NULL,
     `updated_at` datetime NOT NULL,
     `version` bigint(20) NOT NULL,
     PRIMARY KEY (`id`),
     UNIQUE KEY `uniq_routeid` (`route_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;

-- gateway.gateway_route_args definition

CREATE TABLE `gateway_route_args` (
      `id` bigint(20) NOT NULL AUTO_INCREMENT,
      `route_id` varchar(100) NOT NULL,
      `type` varchar(50) NOT NULL,
      `name` varchar(100) NOT NULL,
      `args` text NOT NULL,
      `created_at` datetime NOT NULL,
      `updated_at` datetime NOT NULL,
      `version` bigint(20) NOT NULL,
      PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4;
