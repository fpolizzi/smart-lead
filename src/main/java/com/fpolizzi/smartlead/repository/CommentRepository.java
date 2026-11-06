package com.fpolizzi.smartlead.repository;

import com.fpolizzi.smartlead.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
