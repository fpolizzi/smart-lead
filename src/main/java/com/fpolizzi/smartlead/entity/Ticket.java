package com.fpolizzi.smartlead.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fpolizzi.smartlead.entity.enums.Category;
import com.fpolizzi.smartlead.entity.enums.LeadType;
import com.fpolizzi.smartlead.entity.enums.Priority;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeadType leadType;

    /** Support subcategory; only set when leadType is SUPPORT. */
    @Enumerated(EnumType.STRING)
    private Category category;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String summary;

    @OneToOne(optional = false)
    @JoinColumn(name = "comment_id", nullable = false, unique = true)
    private Comment comment;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Ticket() {
    }

    public Ticket(String title, LeadType leadType, Category category, Priority priority, String summary, Comment comment) {
        this.title = title;
        this.leadType = leadType;
        this.category = category;
        this.priority = priority;
        this.summary = summary;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LeadType getLeadType() {
        return leadType;
    }

    public void setLeadType(LeadType leadType) {
        this.leadType = leadType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @AssertTrue(message = "category is only allowed for support leads")
    @JsonIgnore
    public boolean isCategoryValid() {
        return category == null || leadType == LeadType.SUPPORT;
    }
}
