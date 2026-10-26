package com.fpolizzi.smartlead.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Created by fpolizzi on 10/25/26
 */
@Entity
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String senderEmail;

    @Column(length = 2000)
    private String content;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
