package com.mgumieniak.spring.kafka.webapp.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Data
@Entity
@EqualsAndHashCode(exclude = {"id"})
public class Post {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String name;
}
