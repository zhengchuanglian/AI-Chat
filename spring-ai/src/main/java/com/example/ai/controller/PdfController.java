package com.example.ai.controller;

import com.example.ai.respository.ChatHistoryRepository;
import com.example.ai.respository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 聊天控制器 - 处理普通聊天请求的 REST API 端点
 * 提供与 AI 助手（蜡笔小新角色）进行对话的功能，支持流式响应和对话历史保存
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {
    private final FileRepository fileRepository;
    private final ChatClient chatClient;  // 注入普通聊天客户端，用于处理对话请求
    private final ChatHistoryRepository chatHistoryRepository;  // 注入聊天历史仓库，用于保存会话记录
    private final ChatClient pdfChatClient;
    private final VectorStore vectorStore;
    private final MessageWindowChatMemory chatMemory;

    /**
     * 上传PDF文件
     * 接收用户上传的PDF文件并保存，建立chatId与文件的映射关系
     *
     * @param file 用户上传的PDF文件
     * @param chatId 会话 ID，用于标识和跟踪特定的对话上下文
     * @return ResponseEntity<Map<String, Object>> 上传结果，包含成功状态和文件名
     */
    @PostMapping("/upload/{chatId}")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file, @PathVariable("chatId") String chatId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 将MultipartFile转换为Resource
            Resource resource = new InputStreamResource(file.getInputStream()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            // 保存文件
            boolean success = fileRepository.save(chatId, resource);
            if (success) {
                result.put("success", true);
                result.put("message", "文件上传成功");
                result.put("fileName", file.getOriginalFilename());
            } else {
                result.put("success", false);
                result.put("message", "文件保存失败");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 处理聊天请求 - 流式返回 AI 响应
     * 该方法接收用户输入，保存到历史记录，然后调用 AI 模型生成回复
     *
     * @param prompt 用户输入的聊天内容（问题或消息）
     * @param chatId 会话 ID，用于标识和跟踪特定的对话上下文
     * @return Flux<String> 响应式流，逐块返回 AI 生成的文本内容（Server-Sent Events 格式）
     */
    @RequestMapping(value = "/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId) {
        //0.找到会话文件
        Resource file = fileRepository.getFile(chatId);
        if (!file.exists()){
            throw new RuntimeException("未找到会话文件");
        }

        //1.保存会话id - 将当前会话信息存储到历史记录中，便于后续查询和管理
        chatHistoryRepository.save("pdf",chatId);
        
        //2.创建动态的QuestionAnswerAdvisor，根据文件名过滤
        String fileName = file.getFilename();
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .filterExpression("file_name == '" + fileName + "'")
                        .similarityThreshold(0.1)  // 降低相似度阈值，确保能检索到内容
                        .topK(5)  // 返回前5个相关文档
                        .build())
                .build();
        
        //3.请求模型 - 调用 AI 模型生成回复，使用流式输出提升用户体验
        return pdfChatClient.prompt()
                .user(prompt)  // 设置用户输入的消息内容
                .advisors(a -> a.param(CONVERSATION_ID, chatId))  // 传入会话 ID，启用对话记忆功能，保持上下文连贯性
                .advisors(qaAdvisor)  // 添加动态的QuestionAnswerAdvisor
                .stream()  // 启用流式输出模式，实现逐字显示效果
                .content();  // 返回纯文本内容流
    }
}
