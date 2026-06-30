package com.example.ai.controller;


import com.example.ai.entity.vo.MessageV0;
import com.example.ai.respository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天历史控制器 - 管理对话历史的 REST API 端点
 * 提供查询会话列表和获取特定会话完整聊天记录的功能，支持前端展示历史对话内容
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {
    private final ChatHistoryRepository chatHistoryRepository;  // 注入聊天历史仓库，用于查询会话列表

    public final ChatMemory chatMemory;  // 注入聊天记忆组件，用于获取完整的对话消息记录

    /**
     * 获取指定类型的所有会话 ID 列表
     * 用于在前端展示用户的历史会话清单，方便用户选择要查看的对话
     *
     * @param type   会话类型（如 "chat" 表示普通聊天）
     * @param chatId （未使用参数，可移除）
     * @return List<String> 该类型下的所有会话 ID 集合
     */
    @RequestMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type, String chatId) {
        return chatHistoryRepository.getAllChatIds( type);
    }

    /**
     * 获取指定会话的完整聊天记录
     * 从聊天记忆中提取该会话的所有消息，并转换为前端友好的 VO 对象格式
     *
     * @param type   会话类型（路径变量，当前未实际使用）
     * @param chatId 目标会话的唯一标识符
     * @return List<MessageV0> 转换后的消息列表，包含角色和内容信息；如果会话不存在则返回空列表
     */
    @RequestMapping("/{type}/{chatId}")
    public List<MessageV0> getHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        // 从聊天记忆中获取指定会话的所有消息记录
        List<Message> messages = chatMemory.get(chatId);
        if (messages == null){
            // 如果该会话没有历史记录，返回空列表而非 null，避免前端处理异常
            return List.of();
        }
        // 将 Spring AI 的 Message 对象转换为自定义的 MessageV0 对象，便于前端展示
        return messages.stream().map(MessageV0::new).toList();
    }
}
