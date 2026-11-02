package com.kornfeld.notifier.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class DataChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean backwardIncompatible;

    private LocalDateTime timestamp;

    private String usageExamples;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    private String prevCommitShortSha;

    private String latestCommitShortSha;

    @Column(columnDefinition = "TEXT")
    private String openApiOld;

    @Column(columnDefinition = "TEXT")
    private String openApiNew;
}