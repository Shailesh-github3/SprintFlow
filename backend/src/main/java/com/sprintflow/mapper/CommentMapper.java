package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.sprintflow.entity.Comment;
import com.sprintflow.dto.CommentDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    @Mapping(target = "issueId", source = "issue.issueId")
    CommentDTO toDto(Comment comment);
}
