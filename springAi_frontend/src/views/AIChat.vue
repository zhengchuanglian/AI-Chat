<script setup>
import { ref, nextTick, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const messagesContainer = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const selectedFiles = ref([])
const fileInput = ref(null)
const sidebarOpen = ref(false)

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function generateChatId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

function generateChatTitle(firstMessage) {
  return firstMessage.slice(0, 20) + (firstMessage.length > 20 ? '...' : '')
}

const chatSessions = ref([])
const currentChatId = ref('')

const currentChat = computed(() => {
  return chatSessions.value.find(chat => chat.id === currentChatId.value)
})

const currentScore = computed(() => {
  return currentChat.value?.score || 0
})

const currentMessages = computed(() => {
  return currentChat.value?.messages || []
})

const loadChatHistory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/ai/history/chat`)
    const chatIds = await response.json()

    if (chatIds && chatIds.length > 0) {
      const sessions = []
      for (const chatId of chatIds) {
        const msgResponse = await fetch(`${API_BASE_URL}/ai/history/chat/${encodeURIComponent(chatId)}`)
        const messages = await msgResponse.json()

        const formattedMessages = messages.map(msg => ({
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content,
          score: msg.score
        }))

        if (formattedMessages.length === 0) {
          formattedMessages.push({
            role: 'assistant',
            content: '你好！我是AI助手小新，有什么可以帮助你的吗？',
            score: 0
          })
        }

        const firstUserMsg = messages.find(m => m.role === 'user')
        const title = firstUserMsg ? generateChatTitle(firstUserMsg.content) : '新对话'

        sessions.push({
          id: chatId,
          title: title,
          messages: formattedMessages,
          createdAt: Date.now(),
          score: 0
        })
      }

      chatSessions.value = sessions.reverse()
      currentChatId.value = chatSessions.value[0].id
    } else {
      createNewChat()
    }
  } catch (error) {
    console.error('加载历史会话失败:', error)
    createNewChat()
  }
}

const createNewChat = () => {
  const newChat = {
    id: generateChatId(),
    title: '新对话',
    messages: [
      { role: 'assistant', content: '你好！我是AI助手小新，有什么可以帮助你的吗？', score: 0 }
    ],
    createdAt: Date.now(),
    score: 0
  }
  chatSessions.value.unshift(newChat)
  currentChatId.value = newChat.id
  inputMessage.value = ''
  sidebarOpen.value = false
}

const switchChat = async (chatId) => {
  currentChatId.value = chatId
  inputMessage.value = ''
  sidebarOpen.value = false

  const chat = chatSessions.value.find(c => c.id === chatId)
  if (chat) {
    try {
      const response = await fetch(`${API_BASE_URL}/ai/history/chat/${encodeURIComponent(chatId)}`)
      const messages = await response.json()

      if (messages && messages.length > 0) {
        chat.messages = messages.map(msg => ({
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content,
          score: msg.score
        }))
      }
    } catch (error) {
      console.error('加载消息失败:', error)
    }
  }

  nextTick(() => scrollToBottom())
}

onMounted(() => {
  loadChatHistory()
})

const deleteChat = (chatId, event) => {
  event.stopPropagation()
  const index = chatSessions.value.findIndex(chat => chat.id === chatId)
  if (index === -1) return

  chatSessions.value.splice(index, 1)

  if (chatSessions.value.length === 0) {
    createNewChat()
  } else if (currentChatId.value === chatId) {
    currentChatId.value = chatSessions.value[0].id
  }
}

const updateChatTitle = (chatId, firstUserMessage) => {
  const chat = chatSessions.value.find(c => c.id === chatId)
  if (chat && chat.title === '新对话') {
    chat.title = generateChatTitle(firstUserMessage)
  }
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  selectedFiles.value = [...selectedFiles.value, ...files]
  event.target.value = ''
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const sendMessage = async () => {
  if ((!inputMessage.value.trim() && selectedFiles.value.length === 0) || loading.value) return

  const userMessage = inputMessage.value.trim()
  const chat = currentChat.value

  if (chat.messages.length === 1) {
    updateChatTitle(chat.id, userMessage || '文件对话')
  }

  const fileNames = selectedFiles.value.map(f => f.name).join(', ')
  const displayContent = userMessage + (fileNames ? `\n[附件: ${fileNames}]` : '')
  chat.messages.push({ role: 'user', content: displayContent })
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  const assistantMessage = { role: 'assistant', content: '', score: 0 }
  chat.messages.push(assistantMessage)

  try {
    if (selectedFiles.value.length > 0) {
      const formData = new FormData()
      formData.append('prompt', userMessage)
      formData.append('chatId', chat.id)
      selectedFiles.value.forEach(file => {
        formData.append('files', file)
      })

      selectedFiles.value = []

      const response = await fetch(`${API_BASE_URL}/ai/chat`, {
        method: 'POST',
        body: formData
      })

      if (!response.ok) {
        throw new Error('请求失败')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data && data !== '[DONE]') {
              assistantMessage.content += data
              nextTick(() => scrollToBottom())
            }
          }
        }
      }

      loading.value = false
    } else {
      const eventSource = new EventSource(
        `${API_BASE_URL}/ai/chat?prompt=${encodeURIComponent(userMessage)}&chatId=${encodeURIComponent(chat.id)}`
      )

      eventSource.onmessage = (event) => {
        assistantMessage.content += event.data
        nextTick(() => scrollToBottom())
      }

      eventSource.addEventListener('score', (e) => {
        const score = parseInt(e.data)
        if (!isNaN(score)) {
          assistantMessage.score = score
          chat.score = score
          nextTick(() => scrollToBottom())
        }
      })

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
    }
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

const goBack = () => {
  router.push('/')
}

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}
</script>

<template>
  <div class="ai-chat">
    <aside :class="['sidebar', { active: sidebarOpen }]">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="createNewChat">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>新建对话</span>
        </button>
      </div>

      <div class="chat-list">
        <div
          v-for="chat in chatSessions"
          :key="chat.id"
          :class="['chat-item', { active: chat.id === currentChatId }]"
          @click="switchChat(chat.id)"
        >
          <svg class="chat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="9" y1="9" x2="15" y2="9"></line>
            <line x1="9" y1="15" x2="15" y2="15"></line>
          </svg>
          <span class="chat-title">{{ chat.title }}</span>
          <button class="delete-btn" @click="deleteChat(chat.id, $event)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>

      <div class="sidebar-footer">
        <button class="back-home-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
          <span>返回首页</span>
        </button>
      </div>
    </aside>

    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <button class="menu-btn" @click="toggleSidebar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
          </button>
          <span class="header-title">{{ currentChat?.title || 'AI 聊天' }}</span>
        </div>
        <div class="logo">AI Hub</div>
      </header>

      <div class="chat-container">
        <div class="messages" ref="messagesContainer">
          <div
            v-for="(message, index) in currentMessages"
            :key="index"
            :class="['message', message.role]"
          >
            <div class="avatar">
              <svg v-if="message.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="9" y1="9" x2="15" y2="9"></line>
                <line x1="9" y1="15" x2="15" y2="15"></line>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="content">
              <div class="bubble">{{ message.content }}</div>
            </div>
          </div>
          <div v-if="loading" class="message assistant">
            <div class="avatar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="9" y1="9" x2="15" y2="9"></line>
                <line x1="9" y1="15" x2="15" y2="15"></line>
              </svg>
            </div>
            <div class="content">
              <div class="bubble loading">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div v-if="selectedFiles.length > 0" class="file-preview-area">
            <div v-for="(file, index) in selectedFiles" :key="index" class="file-tag">
              <span class="file-name">{{ file.name }}</span>
              <button class="remove-file-btn" @click="removeFile(index)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
          <div class="input-wrapper">
            <input
              ref="fileInput"
              type="file"
              multiple
              style="display: none"
              @change="handleFileSelect"
            />
            <button class="attach-btn" @click="triggerFileInput" title="上传文件">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
              </svg>
            </button>
            <textarea
              v-model="inputMessage"
              placeholder="输入消息..."
              @keydown="handleKeydown"
              rows="1"
            ></textarea>
            <button
              class="send-btn"
              :disabled="(!inputMessage.trim() && selectedFiles.length === 0) || loading"
              @click="sendMessage"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </main>

    <div class="score-display" v-if="currentChat">
      得分: {{ currentScore }}
    </div>
  </div>
</template>

<style scoped>
.ai-chat {
  height: 100vh;
  height: 100dvh;
  display: flex;
  background: #f5f7fa;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: #202123;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 1001;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #2d2d2d;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #4a4b4d;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #2d2d2d;
  border-color: #00bcd4;
}

.new-chat-btn svg {
  width: 16px;
  height: 16px;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #ececf1;
  font-size: 14px;
  transition: all 0.2s;
  margin-bottom: 2px;
  -webkit-tap-highlight-color: transparent;
}

.chat-item:hover {
  background: #2d2d2d;
}

.chat-item.active {
  background: #343541;
}

.chat-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  opacity: 0.7;
}

.chat-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
  color: #ececf1;
}

.chat-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #4a4b4d;
}

.delete-btn svg {
  width: 14px;
  height: 14px;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #2d2d2d;
}

.back-home-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #ececf1;
  font-size: 14px;
  transition: all 0.2s;
}

.back-home-btn:hover {
  background: #2d2d2d;
}

.back-home-btn svg {
  width: 16px;
  height: 16px;
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  color: #666;
}

.menu-btn svg {
  width: 20px;
  height: 20px;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #00bcd4;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  padding: 0 20px 20px;
  min-height: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
}

.message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .avatar {
  background: linear-gradient(135deg, #00bcd4 0%, #008ba3 100%);
  color: white;
}

.message.user .avatar {
  background: #e3f2fd;
  color: #1976d2;
}

.avatar svg {
  width: 20px;
  height: 20px;
}

.content {
  max-width: 75%;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message.assistant .bubble {
  background: white;
  color: #333;
  border: 1px solid #e8e8e8;
  border-top-left-radius: 4px;
}

.message.user .bubble {
  background: linear-gradient(135deg, #00bcd4 0%, #008ba3 100%);
  color: white;
  border-top-right-radius: 4px;
}

.bubble.loading {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 60px;
  justify-content: center;
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

.input-area {
  padding-top: 12px;
  flex-shrink: 0;
}

.file-preview-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 0;
}

.file-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e3f2fd;
  border-radius: 8px;
  font-size: 13px;
  color: #1976d2;
}

.file-name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-file-btn {
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.remove-file-btn:hover {
  background: #bbdefb;
}

.remove-file-btn svg {
  width: 12px;
  height: 12px;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  background: white;
  border-radius: 16px;
  padding: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8e8e8;
  align-items: flex-end;
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 10px 14px;
  font-size: 15px;
  font-family: inherit;
  max-height: 120px;
  min-height: 24px;
}

.input-wrapper textarea::placeholder {
  color: #aaa;
}

.attach-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #666;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.attach-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.attach-btn svg {
  width: 20px;
  height: 20px;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: linear-gradient(135deg, #00bcd4 0%, #008ba3 100%);
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
  box-shadow: 0 4px 12px rgba(0, 188, 212, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.score-display {
  position: absolute;
  top: 60px;
  right: 20px;
  background: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  color: #333;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 10;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    width: 280px;
    height: 100%;
    height: 100dvh;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  .sidebar.active {
    transform: translateX(0);
  }
  
  .menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .header {
    padding: 10px 15px;
  }
  
  .header-title {
    font-size: 14px;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .logo {
    font-size: 16px;
  }
  
  .chat-container {
    padding: 0 10px 10px;
  }
  
  .messages {
    padding: 15px 0;
    gap: 15px;
  }
  
  .message {
    gap: 8px;
  }
  
  .avatar {
    width: 32px;
    height: 32px;
  }
  
  .avatar svg {
    width: 18px;
    height: 18px;
  }
  
  .content {
    max-width: calc(100% - 50px);
  }
  
  .bubble {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .input-wrapper {
    padding: 6px;
    border-radius: 12px;
  }
  
  .input-wrapper textarea {
    padding: 8px 10px;
    font-size: 14px;
  }
  
  .attach-btn, .send-btn {
    width: 36px;
    height: 36px;
  }
  
  .attach-btn svg, .send-btn svg {
    width: 16px;
    height: 16px;
  }
  
  .score-display {
    top: 55px;
    right: 10px;
    font-size: 12px;
    padding: 4px 10px;
  }
  
  .file-preview-area {
    padding: 6px 0 12px 0;
  }
  
  .file-tag {
    font-size: 12px;
    padding: 4px 8px;
  }
  
  .file-name {
    max-width: 100px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 8px 12px;
  }
  
  .header-title {
    font-size: 13px;
    max-width: 120px;
  }
  
  .logo {
    font-size: 14px;
  }
  
  .menu-btn {
    width: 32px;
    height: 32px;
  }
  
  .menu-btn svg {
    width: 18px;
    height: 18px;
  }
  
  .chat-container {
    padding: 0 8px 8px;
  }
  
  .messages {
    padding: 10px 0;
    gap: 12px;
  }
  
  .avatar {
    width: 28px;
    height: 28px;
    border-radius: 6px;
  }
  
  .avatar svg {
    width: 16px;
    height: 16px;
  }
  
  .content {
    max-width: calc(100% - 40px);
  }
  
  .bubble {
    padding: 8px 12px;
    font-size: 13px;
    line-height: 1.5;
    border-radius: 10px;
  }
  
  .message.assistant .bubble {
    border-top-left-radius: 4px;
  }
  
  .message.user .bubble {
    border-top-right-radius: 4px;
  }
  
  .input-area {
    padding-top: 8px;
  }
  
  .input-wrapper {
    padding: 5px;
    gap: 6px;
  }
  
  .input-wrapper textarea {
    padding: 6px 8px;
    font-size: 13px;
    min-height: 36px;
  }
  
  .attach-btn, .send-btn {
    width: 32px;
    height: 32px;
    border-radius: 8px;
  }
  
  .attach-btn svg, .send-btn svg {
    width: 14px;
    height: 14px;
  }
  
  .score-display {
    top: 50px;
    right: 8px;
    font-size: 11px;
    padding: 3px 8px;
  }
  
  .file-tag {
    font-size: 11px;
    padding: 3px 6px;
  }
  
  .file-name {
    max-width: 80px;
  }
  
  .remove-file-btn {
    width: 16px;
    height: 16px;
  }
  
  .remove-file-btn svg {
    width: 10px;
    height: 10px;
  }
}

@media (max-width: 360px) {
  .header-title {
    font-size: 12px;
    max-width: 100px;
  }
  
  .logo {
    font-size: 13px;
  }
  
  .bubble {
    font-size: 12px;
    padding: 7px 10px;
  }
  
  .input-wrapper textarea {
    font-size: 12px;
  }
}
</style>
