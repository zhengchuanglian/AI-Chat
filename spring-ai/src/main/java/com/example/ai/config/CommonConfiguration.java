package com.example.ai.config;

import com.example.ai.constants.SystemConstant;
import com.example.ai.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用配置类 - Spring Bean 配置中心
 * 负责创建和管理聊天相关的核心组件，包括：
 * 1. 聊天记忆存储（用于保存对话历史）
 * 2. 普通聊天客户端（蜡笔小新角色）
 * 3. 游戏聊天客户端（专用游戏场景）
 */
@Configuration
public class CommonConfiguration {

    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 创建消息窗口聊天记忆 Bean
     * 使用 Redis 存储方式保存对话历史，支持服务重启后数据不丢失
     *
     * @param chatMemoryRepository Redis 聊天记忆仓库
     * @return MessageWindowChatMemory 实例，用于管理多轮对话的上下文记忆
     */
    @Bean
    public MessageWindowChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }

    /**
     * 创建普通聊天客户端 Bean - 蜡笔小新角色
     * 配置了系统提示词、日志记录和对话记忆功能
     *
     * @param model      OpenAI 聊天模型，提供 AI 能力
     * @param chatMemory 聊天记忆组件，用于保存和恢复对话上下文
     * @return ChatClient 实例，用于处理普通聊天请求
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel  model,MessageWindowChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                // 设置系统提示词，定义 AI 助手的角色为"蜡笔小新"
                .defaultSystem("你是一个热心的AI助手，你的名字叫蜡笔小新。你是一个5岁的小男孩，说话幽默、调皮、有时天真烂漫，喜欢用独特的语气和人交流。")
                .defaultAdvisors(
                        // 添加简单的日志记录器，用于调试和监控
                        new SimpleLoggerAdvisor(),
                        // 添加对话记忆顾问，实现多轮对话的上下文保持
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }



    /**
     * 创建游戏聊天客户端 Bean - 专用游戏场景
     * 与普通聊天客户端类似，但使用游戏规则作为系统提示词
     * @param model      OpenAI 聊天模型，提供 AI 能力
     * @param chatMemory 聊天记忆组件，用于保存和恢复游戏对话上下文
     * @return ChatClient 实例，专门用于处理游戏相关的聊天请求
     */
    @Bean
    public ChatClient gameChatClient(OpenAiChatModel  model,MessageWindowChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                // 使用游戏规则作为系统提示词，确保 AI 遵循游戏逻辑
                .defaultSystem(SystemConstant.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加简单的日志记录器，用于调试和监控
                        new SimpleLoggerAdvisor(),
                        // 添加对话记忆顾问，实现游戏过程中的上下文保持
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 创建客服聊天客户端 Bean - 专用客服场景
     * 与普通聊天客户端类似，但使用客服规则作为系统提示词
     * 并注册了课程查询工具，让 AI 可以自动查询数据库中的课程和校区信息
     * @param model      OpenAI 聊天模型，提供 AI 能力
     * @param chatMemory 聊天记忆组件，用于保存和恢复客服对话上下文
     * @param courseTools 课程工具类，提供课程和校区查询功能
     * @return ChatClient 实例，专门用于处理客服相关的聊天请求
     */
    @Bean
    public ChatClient customerServiceChatClient(OpenAiChatModel  model,MessageWindowChatMemory chatMemory, CourseTools courseTools) {
        return ChatClient
                .builder(model)
                // 使用客服规则作为系统提示词，确保 AI 遵循客服逻辑
                .defaultSystem(SystemConstant.CUSTOMER_SERVICE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加简单的日志记录器，用于调试和监控
                        new SimpleLoggerAdvisor(),
                        // 添加对话记忆顾问，实现客服过程中的上下文保持
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(courseTools)
                .build();
    }

    @Bean
    public ChatClient pdfChatClient(OpenAiChatModel  model, MessageWindowChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                // 使用客服规则作为系统提示词，确保 AI 遵循客服逻辑
                .defaultSystem("请根据上下文回答问题，禁止随意编造")
                .defaultAdvisors(
                        // 添加简单的日志记录器，用于调试和监控
                        new SimpleLoggerAdvisor(),
                        // 添加对话记忆顾问，实现客服过程中的上下文保持
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
