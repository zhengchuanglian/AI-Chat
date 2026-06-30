package com.example.ai.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 游戏控制器 - 处理游戏场景聊天请求的 REST API 端点
 * 提供与 AI 进行游戏互动的功能，使用专门的游戏系统提示词和规则配置，支持流式响应
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class GameController {

    private final ChatClient gameChatClient;  // 注入游戏专用聊天客户端，配置了游戏规则提示词

    /**
     * 处理游戏聊天请求 - 流式返回 AI 游戏响应
     * 该方法接收用户在游戏中的输入，调用配置了游戏规则的游戏 AI 模型生成回复
     *
     * @param prompt 用户在游戏中输入的指令或对话内容
     * @param chatId 游戏会话 ID，用于跟踪特定游戏实例的上下文状态
     * @return Flux<String> 响应式流，逐块返回 AI 生成的游戏文本内容（Server-Sent Events 格式）
     */
    @RequestMapping(value = "/game", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId) {
        //2.请求模型 - 调用游戏 AI 模型生成回复，使用流式输出提升游戏体验
        return gameChatClient.prompt()
                .user(prompt)  // 设置用户在游戏中的输入内容
                .advisors(a -> a.param(CONVERSATION_ID, chatId))  // 传入游戏会话 ID，保持游戏状态的连续性
                .stream()  // 启用流式输出模式，实现实时反馈效果
                .content()  // 返回纯文本内容流，前端可逐字显示增强沉浸感
                .concatWith(Flux.just("[CLOSE]"));  // 在流结束时发送关闭标记，通知前端连接结束
    }
}
