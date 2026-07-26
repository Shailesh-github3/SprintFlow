package com.sprintflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintflow.entity.Invitation;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    // You can define custom query methods here if needed

    Invitation findByToken(String token);
    Invitation findByUserEmail(String userEmail);
    

}
