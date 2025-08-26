package com.xinyu.InterviewCoach_v2.util;

import com.xinyu.InterviewCoach_v2.dto.*;
import com.xinyu.InterviewCoach_v2.dto.core.*;
import com.xinyu.InterviewCoach_v2.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO转换工具类
 */
@Component
public class DTOConverter {

    /**
     * 将User实体转换为UserDTO
     */
    public UserDTO convertToUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * 批量转换User实体列表为UserDTO列表
     */
    public List<UserDTO> convertToUserDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将Question实体转换为QuestionDTO
     */
    public QuestionDTO convertToQuestionDTO(Question question) {
        if (question == null) {
            return null;
        }
        return new QuestionDTO(
                question.getId(),
                question.getText(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    /**
     * 批量转换Question实体列表为QuestionDTO列表
     */
    public List<QuestionDTO> convertToQuestionDTOList(List<Question> questions) {
        if (questions == null) {
            return null;
        }
        return questions.stream()
                .map(this::convertToQuestionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将Tag实体转换为TagDTO
     */
    public TagDTO convertToTagDTO(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }

    /**
     * 批量转换Tag实体列表为TagDTO列表
     */
    public List<TagDTO> convertToTagDTOList(List<Tag> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(this::convertToTagDTO)
                .collect(Collectors.toList());
    }


    public SessionDTO convertToSessionDTO(Session session) {
        if (session == null) {
            return null;
        }

        SessionDTO dto = new SessionDTO();
        dto.setId(session.getId());
        dto.setUserId(session.getUserId());
        dto.setMode(session.getMode());
        dto.setExpectedQuestionCount(session.getExpectedQuestionCount());
        dto.setAskedQuestionCount(session.getAskedQuestionCount());
        dto.setCompletedQuestionCount(session.getCompletedQuestionCount());
        dto.setStartedAt(session.getStartedAt());
        dto.setEndedAt(session.getEndedAt());
        dto.setIsActive(session.getIsActive());

        // 新增队列相关字段转换
        dto.setQuestionQueue(session.getQuestionQueue());
        dto.setCurrentQuestionId(session.getCurrentQuestionId());
        dto.setQueuePosition(session.getQueuePosition());

        return dto;
    }

    /**
     * 转换SessionDTO为Session实体 - 更新版本
     */
    public Session convertToSessionEntity(SessionDTO dto) {
        if (dto == null) {
            return null;
        }

        Session session = new Session();
        session.setId(dto.getId());
        session.setUserId(dto.getUserId());
        session.setMode(dto.getMode());
        session.setExpectedQuestionCount(dto.getExpectedQuestionCount());
        session.setAskedQuestionCount(dto.getAskedQuestionCount());
        session.setCompletedQuestionCount(dto.getCompletedQuestionCount());
        session.setStartedAt(dto.getStartedAt());
        session.setEndedAt(dto.getEndedAt());
        session.setIsActive(dto.getIsActive());

        // 新增队列相关字段转换
        session.setQuestionQueue(dto.getQuestionQueue());
        session.setCurrentQuestionId(dto.getCurrentQuestionId());
        session.setQueuePosition(dto.getQueuePosition());

        return session;
    }

    /**
     * 将Message实体转换为MessageDTO
     */
    public MessageDTO convertToMessageDTO(Message message) {
        if (message == null) {
            return null;
        }
        return new MessageDTO(
                message.getId(),
                message.getSessionId(),
                message.getType(),
                message.getText(),
                message.getCreatedAt()
        );
    }

    public MessageDTO convertMessageToDTO(Message message) {
        return new MessageDTO(
                message.getId(),
                message.getSessionId(),
                message.getType(),
                message.getText(),
                message.getCreatedAt()
        );
    }

    /**
     * 批量转换Message实体列表为MessageDTO列表
     */
    public List<MessageDTO> convertToMessageDTOList(List<Message> messages) {
        if (messages == null) {
            return null;
        }
        return messages.stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将QuestionSet实体转换为QuestionSetDTO
     */
    public QuestionSetDTO convertToQuestionSetDTO(QuestionSet questionSet) {
        if (questionSet == null) {
            return null;
        }
        return new QuestionSetDTO(
                questionSet.getId(),
                questionSet.getName(),
                questionSet.getDescription(),
                questionSet.getCreatedAt(),
                questionSet.getUpdatedAt()
        );
    }

    /**
     * 批量转换QuestionSet实体列表为QuestionSetDTO列表
     */
    public List<QuestionSetDTO> convertToQuestionSetDTOList(List<QuestionSet> questionSets) {
        if (questionSets == null) {
            return null;
        }
        return questionSets.stream()
                .map(this::convertToQuestionSetDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将UserAttempt实体转换为UserAttemptDTO
     */
    public UserAttemptDTO convertToUserAttemptDTO(UserAttempt userAttempt) {
        if (userAttempt == null) {
            return null;
        }
        return new UserAttemptDTO(
                userAttempt.getUserId(),
                userAttempt.getQuestionId(),
                userAttempt.getAttemptNumber(),
                userAttempt.getUpdatedAt()
        );
    }

    /**
     * 批量转换UserAttempt实体列表为UserAttemptDTO列表
     */
    public List<UserAttemptDTO> convertToUserAttemptDTOList(List<UserAttempt> userAttempts) {
        if (userAttempts == null) {
            return null;
        }
        return userAttempts.stream()
                .map(this::convertToUserAttemptDTO)
                .collect(Collectors.toList());
    }

    /**
     * 从UserDTO创建User实体（用于新建用户）
     */
    public User convertFromUserDTO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setRole(userDTO.getRole());
        return user;
    }

    /**
     * 从QuestionDTO创建Question实体（用于新建题目）
     */
    public Question convertFromQuestionDTO(QuestionDTO questionDTO) {
        if (questionDTO == null) {
            return null;
        }
        Question question = new Question();
        question.setText(questionDTO.getText());
        return question;
    }

    /**
     * 从TagDTO创建Tag实体（用于新建标签）
     */
    public Tag convertFromTagDTO(TagDTO tagDTO) {
        if (tagDTO == null) {
            return null;
        }
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        return tag;
    }

    /**
     * 将Template实体转换为TemplateDTO
     */
    public TemplateDTO convertToTemplateDTO(Template template) {
        return new TemplateDTO(
                template.getId(),
                template.getName(),
                template.getContent(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    /**
     * 为UserDTO清除敏感信息
     */
    public UserDTO clearSensitiveInfo(UserDTO userDTO) {
        if (userDTO != null) {
            userDTO.setPassword(null);
        }
        return userDTO;
    }

    /**
     * 批量为UserDTO列表清除敏感信息
     */
    public List<UserDTO> clearSensitiveInfoFromList(List<UserDTO> userDTOs) {
        if (userDTOs == null) {
            return null;
        }
        return userDTOs.stream()
                .map(this::clearSensitiveInfo)
                .collect(Collectors.toList());
    }
}