package com.fpolizzi.smartlead.entity;

import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.Priority;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Created by fpolizzi on 10/25/26
 */
@Entity
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(length = 1000)
    private String summary;

    @OneToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Ticket() {
    }

    public Ticket(Long id, String title, Category category, Priority priority, String summary, Comment comment, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.summary = summary;
        this.comment = comment;
        this.createdAt = createdAt;
    }
}
