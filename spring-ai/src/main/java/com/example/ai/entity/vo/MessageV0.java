package com.example.ai.entity.vo;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

/**
 * 消息视图对象（VO）- 用于前端展示的消息数据格式
 * 将 Spring AI 内部的 Message 对象转换为简化的数据结构，包含角色和内容两个核心字段，
 * 便于前端渲染聊天界面和显示对话历史。
 */
@Data
public class MessageV0 {

    private String role;     // 消息角色："user" 表示用户消息，"assistant" 表示 AI 回复，其他为空字符串
    private String content;  // 消息文本内容，即实际发送或接收的文字信息

    /**
     * 构造函数 - 从 Spring AI 的 Message 对象创建 MessageV0 实例
     * 根据消息类型映射对应的角色标识，并提取文本内容。
     *
     * @param message Spring AI 原始消息对象，包含完整的消息元数据和内容
     */
    public MessageV0(Message  message) {
        // 根据消息类型确定角色标识，用于前端区分用户和 AI 的消息样式
        switch (message.getMessageType()) {
            case USER:      // 用户发送的消息，前端通常显示在右侧并使用不同颜色/样式
                role = "user";
                break;
                case ASSISTANT:  // AI 助手回复的消息，前端通常显示在左侧并可能带有头像或特殊标记
                role = "assistant";
                break;
            default:       // 其他类型的消息（如系统消息），统一使用空字符串作为角色标识
                role = "";
                break;
        }
        // 提取消息的文本内容，这是用户实际看到的核心信息部分
        this.content =message.getText();
    }
}
