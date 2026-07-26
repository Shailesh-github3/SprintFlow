package com.sprintflow.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;

@Entity
@Table(indexes = { @Index(name="idx_issue_project", columnList="project_id") })
@Data
public class Issue {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long issueId;

    private String issueTitle;
    private String issueDescription;
    private String issueStatus;
    private String priority;
    private LocalDate dueDate;
    @ElementCollection
    @CollectionTable(name = "issue_tags", joinColumns = @JoinColumn(name = "issue_id"))
    private List<String> tags = new java.util.ArrayList<>();

    @ManyToOne
    private User assignedUser;

    @JsonIgnore
    @ManyToOne
    private Project project;

    @JsonIgnore
    @OneToMany(mappedBy = "issue", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new java.util.ArrayList<>();

    

}
