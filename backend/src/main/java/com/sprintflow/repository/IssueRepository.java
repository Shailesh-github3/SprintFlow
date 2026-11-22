package com.sprintflow.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprintflow.entity.Issue;
import com.sprintflow.entity.User;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @EntityGraph(attributePaths = {"assignedUser", "createdBy"})
    Page<Issue> findByProject_ProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"assignedUser", "createdBy"})
    @Query("SELECT i FROM Issue i WHERE i.project.projectId = :projectId " +
           "AND (:status IS NULL OR i.issueStatus = :status) " +
           "AND (:priority IS NULL OR i.priority = :priority) " +
           "AND (:assigneeId IS NULL OR (i.assignedUser IS NOT NULL AND i.assignedUser.userId = :assigneeId)) " +
           "AND (:keyword IS NULL OR LOWER(i.issueTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Issue> findByProjectWithFilters(@Param("projectId") Long projectId,
                                         @Param("status") String status,
                                         @Param("priority") String priority,
                                         @Param("assigneeId") Long assigneeId,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    List<Issue> findByProject_ProjectIdAndAssignedUser_UserId(Long projectId, Long userId);

    @Query("SELECT COUNT(DISTINCT i) FROM Issue i JOIN i.project p LEFT JOIN p.projectMembers m " +
           "WHERE p.projectOwner = :user OR m = :user")
    long countTotalIssuesForUser(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT i) FROM Issue i JOIN i.project p LEFT JOIN p.projectMembers m " +
           "WHERE (p.projectOwner = :user OR m = :user) AND i.issueStatus = :status")
    long countIssuesForUserByStatus(@Param("user") User user, @Param("status") String status);

    @Query("SELECT COUNT(DISTINCT i) FROM Issue i JOIN i.project p LEFT JOIN p.projectMembers m " +
           "WHERE (p.projectOwner = :user OR m = :user) AND i.dueDate < CURRENT_DATE AND i.issueStatus <> 'DONE'")
    long countOverdueIssuesForUser(@Param("user") User user);

    // Project-specific counts for Reports / Project Stats
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.projectId = :projectId")
    long countTotalIssuesByProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.projectId = :projectId AND i.issueStatus = :status")
    long countIssuesByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.projectId = :projectId AND i.dueDate < CURRENT_DATE AND i.issueStatus <> 'DONE'")
    long countOverdueIssuesByProject(@Param("projectId") Long projectId);

}
