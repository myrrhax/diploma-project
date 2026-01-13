package com.github.myrrhax.diploma_project.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity<K extends Serializable> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    K id;

    @Column(name = "created_at")
    Instant createdAt = Instant.now();
}
