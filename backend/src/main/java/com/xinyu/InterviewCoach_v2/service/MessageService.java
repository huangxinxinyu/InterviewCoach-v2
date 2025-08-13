package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.core.MessageDTO;
import com.xinyu.InterviewCoach_v2.entity.Message;
import com.xinyu.InterviewCoach_v2.enums.MessageType;
import com.xinyu.InterviewCoach_v2.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息业务逻辑层
 */
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 创建新消息
     */
    @Transactional
    public MessageDTO createMessage(Long sessionId, MessageType type, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }

        Message message = new Message(sessionId, type, text);

        int result = messageMapper.insert(message);
        if (result > 0) {
            return convertToDTO(message);
        } else {
            throw new RuntimeException("创建消息失败");
        }
    }

    /**
     * 根据ID查询消息
     */
    public Optional<MessageDTO> getMessageById(Long id) {
        return messageMapper.findById(id).map(this::convertToDTO);
    }

    /**
     * 根据会话ID查询所有消息
     */
    public List<MessageDTO> getMessagesBySessionId(Long sessionId) {
        return messageMapper.findBySessionId(sessionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话的最后一条消息
     */
    public Optional<MessageDTO> getLastMessageBySessionId(Long sessionId) {
        return messageMapper.findLastBySessionId(sessionId).map(this::convertToDTO);
    }

    /**
     * 统计会话消息数量
     */
    public long getMessageCountBySessionId(Long sessionId) {
        return messageMapper.countBySessionId(sessionId);
    }

    /**
     * 统计会话中AI消息数量
     */
    public long getAIMessageCountBySessionId(Long sessionId) {
        return messageMapper.countAIMessagesBySessionId(sessionId);
    }

    /**
     * 统计会话中用户消息数量
     */
    public long getUserMessageCountBySessionId(Long sessionId) {
        return messageMapper.countUserMessagesBySessionId(sessionId);
    }

    /**
     * 删除会话的所有消息
     */
    @Transactional
    public boolean deleteMessagesBySessionId(Long sessionId) {
        return messageMapper.deleteBySessionId(sessionId) >= 0;
    }

    /**
     * 将Message实体转换为MessageDTO
     */
    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(
                message.getId(),
                message.getSessionId(),
                message.getType(),
                message.getText(),
                message.getCreatedAt()
        );
    }
}
