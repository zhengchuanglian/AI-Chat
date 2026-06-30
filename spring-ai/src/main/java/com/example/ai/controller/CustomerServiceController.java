package com.example.ai.controller;

import com.example.ai.respository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class CustomerServiceController {

    private final ChatClient customerServiceChatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/service", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId) {
        //1.保存聊天记录 - 将当前会话信息存储到历史记录中，便于后续查询和管理
        chatHistoryRepository.save("service",chatId);
        //2.请求模型 - 调用 AI 模型生成回复，使用流式输出提升用户体验
        return customerServiceChatClient.prompt()
                .user(prompt)  // 设置用户输入的消息内容
                .advisors(a -> a.param(CONVERSATION_ID, chatId))  // 传入会话 ID，启用对话记忆功能，保持上下文连贯性
                .stream()  // 启用流式输出模式，实现逐字显示效果
                .content();  // 返回纯文本内容流
    }
}
