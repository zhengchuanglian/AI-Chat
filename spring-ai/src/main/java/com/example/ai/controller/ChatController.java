package com.example.ai.controller;

import com.example.ai.respository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 聊天控制器 - 处理普通聊天请求的 REST API 端点
 * 提供与 AI 助手（蜡笔小新角色）进行对话的功能，支持流式响应和对话历史保存
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;  // 注入普通聊天客户端，用于处理对话请求
    private final ChatHistoryRepository chatHistoryRepository;  // 注入聊天历史仓库，用于保存会话记录

    /**
     * 处理聊天请求 - 流式返回 AI 响应
     * 该方法接收用户输入，保存到历史记录，然后调用 AI 模型生成回复
     *
     * @param prompt 用户输入的聊天内容（问题或消息）
     * @param chatId 会话 ID，用于标识和跟踪特定的对话上下文
     * @return Flux<String> 响应式流，逐块返回 AI 生成的文本内容（Server-Sent Events 格式）
     */
    @RequestMapping(value = "/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(@RequestParam("prompt") String prompt,
                             @RequestParam("chatId") String chatId,
                             @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        //1.保存聊天记录 - 将当前会话信息存储到历史记录中，便于后续查询和管理
        chatHistoryRepository.save("chat",chatId);
        //2.请求模型 - 调用 AI 模型生成回复，使用流式输出提升用户体验
        //判断文件是否为空
        if (files==null||files.isEmpty()){
            return textChat(prompt,chatId);
        }else {
            //有附件
            return multiModalChat(prompt,chatId,files);
        }
    }

    private Flux<String> multiModalChat(String prompt, String chatId, List<MultipartFile> files) {
        //1.解析多媒体
        List<Media> media = files.stream()
                .map(file -> new Media(
                        MimeType.valueOf(file.getContentType()),
                        file.getResource()))
                .toList();
        //请求模型
        return chatClient.prompt()
                .user(p->p.text(prompt).media(media.toArray(Media[]::new)))  // 输入用户问题
                .advisors(a -> a.param(CONVERSATION_ID, chatId))  // 传入会话 ID，启用对话记忆功能，保持上下文连贯性
                .stream()  // 启用流式输出模式，实现逐字显示效果
                .content();
    }

    private Flux<String> textChat(String prompt, String chatId) {
        return chatClient.prompt()
                .user(prompt)  // 输入用户问题
                .advisors(a -> a.param(CONVERSATION_ID, chatId))  // 传入会话 ID，启用对话记忆功能，保持上下文连贯性
                .stream()  // 启用流式输出模式，实现逐字显示效果
                .content();  // 返回纯文本内容流
    }
}
