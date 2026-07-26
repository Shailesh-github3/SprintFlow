package com.sprintflow.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long projectId;

    private String projectName;
    private String projectDescription;
    private String category;

    @ElementCollection
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    private List<String> tags = new java.util.ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "project", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private Chat chat;

    @ManyToOne
    private User projectOwner;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Issue> issues = new java.util.ArrayList<>();

    @ManyToMany
    private List<User> projectMembers = new java.util.ArrayList<>();

}
