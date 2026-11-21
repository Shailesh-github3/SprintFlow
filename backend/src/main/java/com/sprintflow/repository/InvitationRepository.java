package com.sprintflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintflow.entity.Invitation;

import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Invitation findByToken(String token);
    Invitation findByUserEmailAndProjectId(String userEmail, Long projectId);
    List<Invitation> findByUserEmail(String userEmail);

}
