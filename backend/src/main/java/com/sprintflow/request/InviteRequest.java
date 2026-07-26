package com.sprintflow.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteRequest {

    @NotNull
    private Long projectId;
    @Email(message = "Email must be valid")
    private String userEmail;

}
