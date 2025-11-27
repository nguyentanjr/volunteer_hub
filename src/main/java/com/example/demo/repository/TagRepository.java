package com.example.demo.repository;

import com.example.demo.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);


    @Query("SELECT t FROM Tag t JOIN t.users u WHERE u.id = :userId")
    Set<Tag> findAllVolunteerTags(Long userId);

}
