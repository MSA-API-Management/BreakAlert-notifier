package net.jamnig.breakalert.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "data_change_examples", joinColumns = @JoinColumn(name = "data_change_id"))
    @MapKeyColumn(name = "endpoint_title")
    @Column(name = "curl_command", columnDefinition = "TEXT")
    private Map<String, String> usageExamples;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    private String prevCommitShortSha;

    private String latestCommitShortSha;

    @Column(columnDefinition = "TEXT")
    private String openApiOld;

    @Column(columnDefinition = "TEXT")
    private String openApiNew;
}