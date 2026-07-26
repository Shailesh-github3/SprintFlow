package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import com.sprintflow.entity.Subscription;
import com.sprintflow.dto.SubscriptionDTO;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionDTO toDto(Subscription subscription);
}
