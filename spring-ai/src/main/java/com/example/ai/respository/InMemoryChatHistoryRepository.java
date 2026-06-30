package com.example.ai.respository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存聊天历史仓库实现 - 基于内存的会话记录存储方案
 * 使用 HashMap 在应用内存中保存和管理会话 ID 列表，按类型分类存储。
 * 优点：性能高、无需外部依赖；缺点：应用重启后数据丢失，适合开发测试或临时会话管理场景。
 */
@Component
public class InMemoryChatHistoryRepository implements ChatHistoryRepository{

    // 核心数据结构：key 为会话类型（如 "chat"），value 为该类型下的所有会话 ID 列表
    private final Map<String,List<String>> chatHistory = new HashMap<>();
    /**
     * 保存会话记录到内存存储中 - 将新的会话 ID 添加到对应类型的列表中
     * 该方法确保每个会话 ID 在同一类型下只会被添加一次，避免重复记录。
     *
     * @param chatId 要保存的会话唯一标识符（注意：参数名与接口定义相反，实际第一个参数是 type）
     * @param type   会话类型分类（注意：参数名与接口定义相反，实际第二个参数是 chatId）
     *               TODO: 修复参数顺序问题，应与接口定义的 save(String type, String chatId) 保持一致
     */
    @Override
    public void save(String chatId, String type) {
//        if (!chatHistory.containsKey(chatId)){
//            chatHistory.put(chatId, List.of(type));
//        }
        // 获取或创建指定类型的会话 ID 列表，如果该类型不存在则自动创建一个新的 ArrayList
        List<String> chatIds = chatHistory.computeIfAbsent(type,k->new ArrayList<>());
        // 检查会话 ID 是否已存在，避免重复添加相同的会话记录（保证数据的唯一性）
        if (chatIds.contains(chatId)){
            return;  // 如果已存在则直接返回，不做任何操作
        }
        // 将新的会话 ID 添加到对应类型的列表中，完成会话记录的保存操作
        chatIds.add(chatId);
    }

    /**
     * 获取指定类型的所有会话 ID 列表 - 查询某类会话的历史记录索引
     * 从内存中检索并返回该类型下保存的所有会话 ID，用于前端展示会话清单。
     *
     * @param type 会话类型筛选条件（如 "chat" 表示普通聊天会话）
     * @return List<String> 该类型下的所有会话 ID 集合；如果该类型没有记录则返回空列表而非 null，避免空指针异常
     */
    @Override
    public List<String> getAllChatIds(String type) {
        // 使用 getOrDefault 确保即使类型不存在也返回空列表，提高代码健壮性
        return chatHistory.getOrDefault(type,new ArrayList<>());
    }
}
