package com.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.entity.ChatMessage;
import com.kb.entity.ChatSession;
import com.kb.mapper.ChatMessageMapper;
import com.kb.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    /**
     * 获取会话列表
     */
    public List<ChatSession> getSessionList(Long userId) {
        List<ChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(userId != null, ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime));

        // 填充消息数量
        for (ChatSession session : sessions) {
            session.setMessageCount(messageMapper.countBySessionId(session.getId()));
        }

        return sessions;
    }

    /**
     * 创建新会话
     */
    @Transactional
    public ChatSession createSession(Long kbId, Long userId) {
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        session.setKbIds(kbId != null ? kbId.toString() : null);
        session.setUserId(userId);
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 更新会话标题
     */
    public void updateSessionTitle(Long sessionId, String title) {
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle(title.length() > 50 ? title.substring(0, 50) + "..." : title);
        sessionMapper.updateById(session);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        // 删除消息
        messageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId));
        // 删除会话
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 获取会话消息
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));
    }

    /**
     * 保存消息
     */
    @Transactional
    public ChatMessage saveMessage(Long sessionId, String role, String content, String sources) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setSources(sources);
        messageMapper.insert(message);

        // 更新会话时间
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        sessionMapper.updateById(session);

        return message;
    }

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionCount", sessionMapper.countTotal());
        stats.put("messageCount", messageMapper.countTotal());
        return stats;
    }
}
