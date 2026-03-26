package com.kornfeld.notifier.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class DataChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String openApiOld;

    @Column(columnDefinition = "TEXT")
    private String openApiNew;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    private LocalDateTime timestamp;

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOpenApiOld() {
        return openApiOld;
    }

    public void setOpenApiOld(String openApiOld) {
        this.openApiOld = openApiOld;
    }

    public String getOpenApiNew() {
        return openApiNew;
    }

    public void setOpenApiNew(String openApiNew) {
        this.openApiNew = openApiNew;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }
}

