package com.sprintflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sprintflow.entity.Issue;


public interface IssueRepository extends JpaRepository<Issue, Long> {
    @EntityGraph(attributePaths = {"comments", "assignedUser"})
    Page<Issue> findByProject_ProjectId(Long projectId, Pageable pageable);
}
