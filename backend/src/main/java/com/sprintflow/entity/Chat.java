package com.sprintflow.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long chatId;

    private String chatName;

    @OneToOne
    private Project project;

    @JsonIgnore
    @OneToMany(mappedBy = "chat", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new java.util.ArrayList<>();

    @ManyToMany
    private List<User> participants = new java.util.ArrayList<>();

}
