<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function generateChatId() {
  return 'service_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

const messages = ref([
  { 
    role: 'assistant', 
    content: '您好！我是智能客服小助手，24小时为您服务。请问有什么可以帮助您的吗？',
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const showQuickReplies = ref(true)
const chatId = ref(generateChatId())

const quickReplies = [
  '如何注册账号？',
  '忘记密码怎么办？',
  '如何联系人工客服？',
  '服务收费标准',
  '功能使用教程'
]

const goBack = () => {
  router.push('/')
}

const sendMessage = async (content = null) => {
  const messageContent = content || inputMessage.value.trim()
  if (!messageContent || loading.value) return
  
  if (!content) {
    inputMessage.value = ''
  }
  
  messages.value.push({ 
    role: 'user', 
    content: messageContent,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  
  loading.value = true
  showQuickReplies.value = false
  
  await nextTick()
  scrollToBottom()
  
  const assistantMessage = { 
    role: 'assistant', 
    content: '', 
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  messages.value.push(assistantMessage)
  
  try {
    const eventSource = new EventSource(
      `${API_BASE_URL}/ai/service?prompt=${encodeURIComponent(messageContent)}&chatId=${encodeURIComponent(chatId.value)}`
    )
    
    eventSource.onmessage = (event) => {
      assistantMessage.content += event.data
      nextTick(() => scrollToBottom())
    }
    
    eventSource.onerror = (error) => {
      eventSource.close()
      loading.value = false
      if (!assistantMessage.content) {
        assistantMessage.content = '抱歉，服务出现了一些问题，请稍后再试。'
      }
    }
    
    eventSource.addEventListener('close', () => {
      eventSource.close()
      loading.value = false
    })
  } catch (error) {
    loading.value = false
    assistantMessage.content = '抱歉，连接服务失败，请检查网络后重试。'
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <div class="customer-service">
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"></path>
          </svg>
        </button>
        <div class="service-info">
          <span class="header-title">智能客服</span>
          <span class="status">
            <span class="status-dot"></span>
            在线
          </span>
        </div>
      </div>
      <div class="logo">AI Hub</div>
    </header>
    
    <div class="service-container">
      <div class="service-sidebar">
        <div class="service-card">
          <div class="service-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <h3>24小时在线服务</h3>
          <p>随时为您解答问题</p>
        </div>
        
        <div class="contact-info">
          <h4>其他联系方式</h4>
          <div class="contact-item">
            <span class="icon">📞</span>
            <span>400-xxx-xxxx</span>
          </div>
          <div class="contact-item">
            <span class="icon">📧</span>
            <span>support@example.com</span>
          </div>
          <div class="contact-item">
            <span class="icon">⏰</span>
            <span>工作时间 9:00-18:00</span>
          </div>
        </div>
      </div>
      
      <div class="chat-container">
        <div class="messages" ref="messagesContainer">
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message', message.role]"
          >
            <div class="avatar">
              <svg v-if="message.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="name">{{ message.role === 'assistant' ? '智能客服' : '您' }}</span>
                <span class="time">{{ message.time }}</span>
              </div>
              <div class="bubble" v-html="message.content.replace(/\n/g, '<br>')"></div>
            </div>
          </div>
          
          <div v-if="loading" class="message assistant">
            <div class="avatar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
              </svg>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="name">智能客服</span>
              </div>
              <div class="bubble loading">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="showQuickReplies" class="quick-replies">
          <p>常见问题：</p>
          <div class="quick-reply-tags">
            <span 
              v-for="(reply, idx) in quickReplies" 
              :key="idx"
              class="quick-reply-tag"
              @click="sendMessage(reply)"
            >
              {{ reply }}
            </span>
          </div>
        </div>
        
        <div class="input-area">
          <div class="input-wrapper">
            <textarea
              v-model="inputMessage"
              placeholder="请输入您的问题..."
              @keydown="handleKeydown"
              rows="1"
            ></textarea>
            <button 
              class="send-btn" 
              :disabled="!inputMessage.trim() || loading"
              @click="sendMessage()"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.customer-service {
  min-height: 100vh;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: #f1f5f9;
}

.back-btn svg {
  width: 20px;
  height: 20px;
  color: #666;
}

.service-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.status {
  font-size: 12px;
  color: #10b981;
  display: flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
}

.service-container {
  flex: 1;
  display: flex;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
  gap: 20px;
}

.service-sidebar {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.service-card {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  padding: 30px;
  border-radius: 16px;
  text-align: center;
}

.service-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 15px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.service-icon svg {
  width: 30px;
  height: 30px;
  color: white;
}

.service-card h3 {
  font-size: 18px;
  margin-bottom: 5px;
}

.service-card p {
  font-size: 14px;
  opacity: 0.9;
}

.contact-info {
  background: white;
  padding: 25px;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.contact-info h4 {
  font-size: 16px;
  color: #333;
  margin-bottom: 15px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  color: #666;
  font-size: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.contact-item:last-child {
  border-bottom: none;
}

.icon {
  font-size: 16px;
}

.chat-container {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 25px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.message.user .avatar {
  background: #eff6ff;
  color: #3b82f6;
}

.avatar svg {
  width: 20px;
  height: 20px;
}

.message-content {
  max-width: 70%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message.assistant .bubble {
  background: #f8fafc;
  color: #333;
  border: 1px solid #e2e8f0;
  border-top-left-radius: 4px;
}

.message.user .bubble {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-top-right-radius: 4px;
}

.bubble.loading {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 60px;
  justify-content: center;
  padding: 15px 20px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.quick-replies {
  padding: 15px 25px;
  border-top: 1px solid #f1f5f9;
}

.quick-replies p {
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
}

.quick-reply-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-reply-tag {
  background: #eff6ff;
  color: #3b82f6;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #dbeafe;
}

.quick-reply-tag:hover {
  background: #3b82f6;
  color: white;
}

.input-area {
  padding: 20px 25px;
  border-top: 1px solid #f1f5f9;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 8px;
  border: 1px solid #e2e8f0;
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 10px 15px;
  font-size: 14px;
  font-family: inherit;
  max-height: 100px;
  min-height: 20px;
  background: transparent;
}

.input-wrapper textarea::placeholder {
  color: #aaa;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .customer-service {
    min-height: 100vh;
    min-height: -webkit-fill-available;
  }
  
  .header {
    padding: 12px 15px;
    position: sticky;
    top: 0;
    z-index: 100;
  }
  
  .header-left {
    gap: 10px;
  }
  
  .header-title {
    font-size: 16px;
  }
  
  .status {
    font-size: 11px;
  }
  
  .status-dot {
    width: 6px;
    height: 6px;
  }
  
  .logo {
    font-size: 18px;
  }
  
  .service-container {
    flex-direction: column;
    padding: 10px;
    gap: 10px;
  }
  
  .service-sidebar {
    width: 100%;
    display: none; /* 默认隐藏侧边栏 */
  }
  
  .service-sidebar.show-mobile {
    display: flex;
    margin-bottom: 15px;
  }
  
  .service-card {
    padding: 20px;
  }
  
  .service-icon {
    width: 50px;
    height: 50px;
    margin-bottom: 10px;
  }
  
  .service-icon svg {
    width: 24px;
    height: 24px;
  }
  
  .service-card h3 {
    font-size: 16px;
  }
  
  .service-card p {
    font-size: 13px;
  }
  
  .contact-info {
    padding: 15px;
  }
  
  .contact-info h4 {
    font-size: 14px;
    margin-bottom: 10px;
  }
  
  .contact-item {
    font-size: 13px;
    padding: 8px 0;
  }
  
  .icon {
    font-size: 14px;
  }
  
  .chat-container {
    border-radius: 12px;
  }
  
  .messages {
    padding: 15px;
  }
  
  .message {
    gap: 8px;
  }
  
  .message-content {
    max-width: calc(100% - 60px);
  }
  
  .bubble {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .message-header {
    margin-bottom: 3px;
  }
  
  .name {
    font-size: 13px;
  }
  
  .time {
    font-size: 11px;
  }
  
  .avatar {
    width: 32px;
    height: 32px;
  }
  
  .avatar svg {
    width: 16px;
    height: 16px;
  }
  
  .quick-replies {
    padding: 12px 15px;
  }
  
  .quick-replies p {
    font-size: 12px;
    margin-bottom: 8px;
  }
  
  .quick-reply-tags {
    gap: 8px;
  }
  
  .quick-reply-tag {
    font-size: 12px;
    padding: 6px 12px;
    flex: 1 0 calc(50% - 8px);
  }
  
  .input-area {
    padding: 15px;
  }
  
  .input-wrapper {
    padding: 6px;
  }
  
  .input-wrapper textarea {
    padding: 8px 10px;
    font-size: 14px;
    min-height: 30px;
  }
  
  .send-btn {
    width: 36px;
    height: 36px;
  }
  
  .send-btn svg {
    width: 16px;
    height: 16px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }
  
  .back-btn {
    width: 32px;
    height: 32px;
  }
  
  .back-btn svg {
    width: 18px;
    height: 18px;
  }
  
  .quick-reply-tag {
    flex: 1 0 100%;
  }
  
  .message-content {
    max-width: calc(100% - 50px);
  }
  
  .message.assistant .bubble {
    border-top-left-radius: 12px;
  }
  
  .message.user .bubble {
    border-top-right-radius: 12px;
  }
  
  .bubble {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .quick-replies {
    padding: 10px 12px;
  }
  
  .input-area {
    padding: 12px;
  }
}
</style>
