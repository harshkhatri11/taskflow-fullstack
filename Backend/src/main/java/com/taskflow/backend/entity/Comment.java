package com.taskflow.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "comments")
@NoArgsConstructor
@Setter
@Getter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User author;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @CreationTimestamp
    private Instant createdAt;

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", task=" + (task != null ? task.getId() : null) +
                ", author=" + (author != null ? author.getId() : null) +
                ", authorName='" + authorName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}