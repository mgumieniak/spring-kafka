package com.mgumieniak.spring.kafka.webapp.repo;

import com.mgumieniak.spring.kafka.webapp.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepo extends JpaRepository<Post, Long> {
}
