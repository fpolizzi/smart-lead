package com.fpolizzi.smartlead.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

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

    public Comment() {
    }

    public Comment(Long id, String senderEmail, String content) {
        this.id = id;
        this.senderEmail = senderEmail;
        this.content = content;
    }

    public Comment(String senderEmail, String content) {
        this.senderEmail = senderEmail;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id) &&
                Objects.equals(senderEmail, comment.senderEmail) &&
                Objects.equals(content, comment.content) &&
                Objects.equals(createdAt, comment.createdAt);
    }

    @Override
    public int hashCode() {
           return Objects.hash(id, senderEmail, content, createdAt);
    }
}
