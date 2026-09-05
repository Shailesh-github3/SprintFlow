package com.sprintflow.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sprintflow.dto.ChatDTO;
import com.sprintflow.dto.CommentDTO;
import com.sprintflow.dto.IssueDTO;
import com.sprintflow.dto.MessageDTO;
import com.sprintflow.dto.ProjectDTO;
import com.sprintflow.dto.SubscriptionDTO;
import com.sprintflow.dto.UserDTO;
import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Comment;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.Message;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        UserMapperImpl.class,
        ChatMapperImpl.class,
        SubscriptionMapperImpl.class,
        CommentMapperImpl.class,
        IssueMapperImpl.class,
        MessageMapperImpl.class,
        ProjectMapperImpl.class
})
class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private IssueMapper issueMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Nested
    @DisplayName("UserMapper")
    class UserMapperTests {

        @Test
        void shouldMapEntityToDtoAndBack() {
            // Given
            User user = new User();
            user.setUserId(1L);
            user.setFullName("Alice Example");
            user.setEmail("alice@example.com");
            user.setProjectSize(3);
            user.setRoles(List.of("USER", "ADMIN"));

            // When
            UserDTO dto = userMapper.toDto(user);
            User mappedBack = userMapper.toEntity(dto);

            // Then
            assertEquals(1L, dto.getUserId());
            assertEquals("Alice Example", dto.getFullName());
            assertEquals(List.of("USER", "ADMIN"), dto.getRoles());
            assertEquals("alice@example.com", mappedBack.getEmail());
            assertEquals(3, mappedBack.getProjectSize());
        }

        @Test
        void shouldReturnNullForNullInput() {
            assertNull(userMapper.toDto(null));
            assertNull(userMapper.toEntity(null));
        }
    }

    @Nested
    @DisplayName("ProjectMapper")
    class ProjectMapperTests {

        @Test
        void shouldMapNestedOwnerChatAndMembers() {
            // Given
            User owner = buildUser(1L, "Owner", "owner@example.com");
            User member = buildUser(2L, "Member", "member@example.com");

            Chat chat = new Chat();
            chat.setChatId(5L);
            chat.setChatName("Sprint chat");
            chat.setParticipants(new ArrayList<>(List.of(member)));

            Project project = new Project();
            project.setProjectId(100L);
            project.setProjectName("SprintFlow");
            project.setProjectDescription("Project tracking");
            project.setCategory("Software");
            project.setTags(List.of("backend", "qa"));
            project.setProjectOwner(owner);
            project.setProjectMembers(new ArrayList<>(List.of(owner, member)));
            project.setChat(chat);

            // When
            ProjectDTO dto = projectMapper.toDto(project);
            Project mappedBack = projectMapper.toEntity(dto);

            // Then
            assertEquals("SprintFlow", dto.getProjectName());
            assertEquals("owner@example.com", dto.getProjectOwner().getEmail());
            assertEquals(2, dto.getProjectMembers().size());
            assertEquals("Sprint chat", dto.getChat().getChatName());
            assertEquals("Software", mappedBack.getCategory());
            assertEquals(2, mappedBack.getProjectMembers().size());
        }

        @Test
        void shouldHandleNullNestedFieldsAndEmptyCollections() {
            // Given
            Project project = new Project();
            project.setProjectId(200L);
            project.setProjectName("Empty Project");
            project.setTags(new ArrayList<>());
            project.setProjectMembers(new ArrayList<>());

            // When
            ProjectDTO dto = projectMapper.toDto(project);

            // Then
            assertEquals(200L, dto.getProjectId());
            assertTrue(dto.getTags().isEmpty());
            assertTrue(dto.getProjectMembers().isEmpty());
            assertNull(dto.getProjectOwner());
            assertNull(dto.getChat());
        }
    }

    @Nested
    @DisplayName("IssueMapper")
    class IssueMapperTests {

        @Test
        void shouldMapIssueWithAssignedUserAndProjectId() {
            // Given
            User assignee = buildUser(3L, "Assignee", "assignee@example.com");
            Issue issue = new Issue();
            issue.setIssueId(10L);
            issue.setIssueTitle("Login bug");
            issue.setIssueDescription("Fix the login flow");
            issue.setIssueStatus("OPEN");
            issue.setPriority("HIGH");
            issue.setDueDate(LocalDate.now().plusDays(7));
            issue.setTags(List.of("security"));
            issue.setAssignedUser(assignee);
            Project project = new Project();
            project.setProjectId(99L);
            issue.setProject(project);

            // When
            IssueDTO dto = issueMapper.toDto(issue);
            Issue mappedBack = issueMapper.toEntity(dto);

            // Then
            assertEquals(99L, dto.getProjectId());
            assertEquals("assignee@example.com", dto.getAssignedUser().getEmail());
            assertEquals("OPEN", mappedBack.getIssueStatus());
            assertEquals(99L, mappedBack.getProject().getProjectId());
        }
    }

    @Nested
    @DisplayName("CommentMapper")
    class CommentMapperTests {

        @Test
        void shouldMapCommentAndIssueReferenceBothWays() {
            // Given
            User creator = buildUser(4L, "Creator", "creator@example.com");
            Issue issue = new Issue();
            issue.setIssueId(88L);

            Comment comment = new Comment();
            comment.setCommentId(55L);
            comment.setCommentText("Looks good");
            comment.setCreatedAt(LocalDateTime.now());
            comment.setCreatedBy(creator);
            comment.setIssue(issue);

            CommentDTO dto = commentMapper.toDto(comment);
            Comment mappedBack = commentMapper.toEntity(dto);

            assertEquals(88L, dto.getIssueId());
            assertEquals("creator@example.com", dto.getCreatedBy().getEmail());
            assertEquals(88L, mappedBack.getIssue().getIssueId());
        }
    }

    @Nested
    @DisplayName("MessageMapper")
    class MessageMapperTests {

        @Test
        void shouldMapMessageAndSenderBothWays() {
            // Given
            User sender = buildUser(5L, "Sender", "sender@example.com");
            Message message = new Message();
            message.setMessageId(77L);
            message.setMessageText("Hello team");
            message.setCreatedAt(LocalDateTime.now());
            message.setSender(sender);

            // When
            MessageDTO dto = messageMapper.toDto(message);
            Message mappedBack = messageMapper.toEntity(dto);

            // Then
            assertEquals("sender@example.com", dto.getSender().getEmail());
            assertEquals("Hello team", mappedBack.getMessageText());
            assertEquals(5L, mappedBack.getSender().getUserId());
        }
    }

    @Nested
    @DisplayName("SubscriptionMapper")
    class SubscriptionMapperTests {

        @Test
        void shouldMapSubscriptionEntityAndDto() {
            // Given
            User user = buildUser(6L, "Subscriber", "subscriber@example.com");
            Subscription subscription = new Subscription();
            subscription.setSubscriptionId(12L);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(LocalDate.now().plusMonths(1));
            subscription.setPlanType(PlanType.YEARLY);
            subscription.setValid(true);
            subscription.setUser(user);

            // When
            SubscriptionDTO dto = subscriptionMapper.toDto(subscription);
            Subscription mappedBack = subscriptionMapper.toEntity(dto);

            // Then
            assertEquals(PlanType.YEARLY, dto.getPlanType());
            assertEquals(12L, dto.getSubscriptionId());
            assertEquals(PlanType.YEARLY, mappedBack.getPlanType());
            assertTrue(mappedBack.isValid());
            assertNull(mappedBack.getUser());
        }
    }

    @Nested
    @DisplayName("ChatMapper")
    class ChatMapperTests {

        @Test
        void shouldMapChatParticipants() {
            // Given
            User participant = buildUser(7L, "Participant", "participant@example.com");
            Chat chat = new Chat();
            chat.setChatId(31L);
            chat.setChatName("Team chat");
            chat.setParticipants(new ArrayList<>(List.of(participant)));

            // When
            ChatDTO dto = chatMapper.toDto(chat);
            Chat mappedBack = chatMapper.toEntity(dto);

            // Then
            assertEquals("Team chat", dto.getChatName());
            assertEquals(31L, dto.getChatId());
            assertEquals("Team chat", mappedBack.getChatName());
            assertTrue(mappedBack.getParticipants().isEmpty());
        }
    }

    private User buildUser(Long userId, String fullName, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setProjectSize(1);
        user.setRoles(List.of("USER"));
        return user;
    }
}