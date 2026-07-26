package com.sprintflow.repository;

import com.sprintflow.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprintflow.entity.User;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByProjectNameContaining(String projectName);
    List<Project> findByProjectOwner(User projectOwner);
    List<Project> findByProjectNameContainingAndProjectMembersContaining(String name, User teamMember);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.projectMembers m WHERE m = :user OR p.projectOwner = :user")
    List<Project> findProjectsForMember(@Param("user") User user);

    @EntityGraph(attributePaths = {"issues", "projectOwner"})
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.projectMembers m WHERE m = :user AND (:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND (:tag IS NULL OR :tag MEMBER OF p.tags)")
    Page<Project> findProjectsForMemberWithFilters(@Param("user") User user, @Param("category") String category, @Param("tag") String tag, Pageable pageable);

}
