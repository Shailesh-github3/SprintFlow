package com.sprintflow.dto;
import lombok.Data;
import java.time.LocalDate;
import com.sprintflow.entity.PlanType;

@Data
public class SubscriptionDTO {
    private Long subscriptionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isValid;
    private PlanType planType;
}
