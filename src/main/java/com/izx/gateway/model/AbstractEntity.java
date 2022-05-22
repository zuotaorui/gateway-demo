package com.izx.gateway.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class AbstractEntity {
    @Id
    @Column("id")
    protected Long id;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;


    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private long version;
}
