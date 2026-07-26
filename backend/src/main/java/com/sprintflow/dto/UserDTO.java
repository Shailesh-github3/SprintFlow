package com.sprintflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    private Long userId;
    private String fullName;
    private String email;
    private int projectSize;
    private List<String> roles;
}
