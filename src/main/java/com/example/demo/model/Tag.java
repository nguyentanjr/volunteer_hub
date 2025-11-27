package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<User> users = new HashSet<>();
}
/*
tag: 0.4
capacity: 0.1
popularity: 0.2
organizer: 0.1
time: 0.2
 */

