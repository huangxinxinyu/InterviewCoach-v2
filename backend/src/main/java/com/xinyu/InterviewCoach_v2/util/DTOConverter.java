package com.xinyu.InterviewCoach_v2.util;

import com.xinyu.InterviewCoach_v2.dto.*;
import com.xinyu.InterviewCoach_v2.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO转换工具类 - 统一处理实体与DTO之间的转换
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

    /**
     * 将Session实体转换为SessionDTO
     */
    public SessionDTO convertToSessionDTO(Session session) {
        if (session == null) {
            return null;
        }
        return new SessionDTO(
                session.getId(),
                session.getUserId(),
                session.getMode(),
                session.getExpectedQuestionCount(),
                session.getAskedQuestionCount(),
                session.getCompletedQuestionCount(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getIsActive()
        );
    }

    /**
     * 批量转换Session实体列表为SessionDTO列表
     */
    public List<SessionDTO> convertToSessionDTOList(List<Session> sessions) {
        if (sessions == null) {
            return null;
        }
        return sessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
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
     * 从QuestionSetDTO创建QuestionSet实体（用于新建题集）
     */
    public QuestionSet convertFromQuestionSetDTO(QuestionSetDTO questionSetDTO) {
        if (questionSetDTO == null) {
            return null;
        }
        QuestionSet questionSet = new QuestionSet();
        questionSet.setName(questionSetDTO.getName());
        questionSet.setDescription(questionSetDTO.getDescription());
        return questionSet;
    }

    /**
     * 更新User实体的字段（从UserDTO）
     */
    public void updateUserFromDTO(User user, UserDTO userDTO) {
        if (user != null && userDTO != null) {
            if (userDTO.getEmail() != null) {
                user.setEmail(userDTO.getEmail());
            }
            if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
                user.setPassword(userDTO.getPassword());
            }
            if (userDTO.getRole() != null) {
                user.setRole(userDTO.getRole());
            }
        }
    }

    /**
     * 更新Question实体的字段（从QuestionDTO）
     */
    public void updateQuestionFromDTO(Question question, QuestionDTO questionDTO) {
        if (question != null && questionDTO != null) {
            if (questionDTO.getText() != null) {
                question.setText(questionDTO.getText().trim());
            }
        }
    }

    /**
     * 更新Tag实体的字段（从TagDTO）
     */
    public void updateTagFromDTO(Tag tag, TagDTO tagDTO) {
        if (tag != null && tagDTO != null) {
            if (tagDTO.getName() != null) {
                tag.setName(tagDTO.getName().trim().toLowerCase());
            }
        }
    }

    /**
     * 更新QuestionSet实体的字段（从QuestionSetDTO）
     */
    public void updateQuestionSetFromDTO(QuestionSet questionSet, QuestionSetDTO questionSetDTO) {
        if (questionSet != null && questionSetDTO != null) {
            if (questionSetDTO.getName() != null) {
                questionSet.setName(questionSetDTO.getName());
            }
            if (questionSetDTO.getDescription() != null) {
                questionSet.setDescription(questionSetDTO.getDescription());
            }
        }
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