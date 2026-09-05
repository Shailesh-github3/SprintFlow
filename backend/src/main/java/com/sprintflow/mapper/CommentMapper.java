package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.sprintflow.entity.Comment;
import com.sprintflow.dto.CommentDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    @Mapping(target = "issueId", source = "issue.issueId")
    CommentDTO toDto(Comment comment);

    @Mapping(target = "issue", source = "issueId")
    Comment toEntity(CommentDTO commentDTO);

    default com.sprintflow.entity.Issue map(Long issueId) {
        if (issueId == null) {
            return null;
        }
        com.sprintflow.entity.Issue issue = new com.sprintflow.entity.Issue();
        issue.setIssueId(issueId);
        return issue;
    }
}
