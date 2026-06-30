package com.example.ai.respository;

import java.util.List;

/**
 * 聊天历史仓库接口 - 定义会话记录管理的标准操作规范
 * 该接口采用仓储模式，抽象了会话记录的持久化逻辑，便于后续切换不同的存储实现（如内存、数据库等）。
 * 当前主要用于跟踪和管理不同类型的聊天会话列表。
 */
public interface ChatHistoryRepository {

    /**
     * 保存会话记录 - 将新的会话添加到历史记录中
     * 当用户创建新对话时调用此方法，确保会话信息被正确记录和索引。
     *
     * @param type   会话类型标识（例如 "chat" 表示普通聊天，"game" 表示游戏会话）
     * @param chatId 会话的唯一标识符，用于后续检索和关联对话内容
     */
    void save(String type,String chatId);
//    String findByChatId(String chatId);
    /**
     * 获取指定类型的所有会话 ID 列表
     * 用于查询某个分类下的所有历史会话，支持前端展示会话清单供用户选择。
     *
     * @param type 会话类型筛选条件，返回该类型下的所有会话 ID
     * @return List<String> 符合条件的会话 ID 集合，按添加顺序排列；如果没有记录则返回空列表
     */
    List<String> getAllChatIds(String  type);
//    void deleteByChatId(String chatId);
}
