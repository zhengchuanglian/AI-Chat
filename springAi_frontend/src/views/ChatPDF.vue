<script setup>
import { ref, nextTick, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const uploadedFile = ref(null)
const isUploading = ref(false)
const messages = ref([])
const inputMessage = ref('')
const pdfContent = ref('')
const loading = ref(false)
const chatId = ref('')
const pdfUrl = ref('')
const currentPage = ref(1)
const totalPages = ref(1)

// PDF会话历史
const pdfSessions = ref([])
const currentPdfId = ref('')

const currentPdfSession = computed(() => {
  return pdfSessions.value.find(session => session.id === currentPdfId.value)
})

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const MAX_PDF_SIZE = 50 * 1024 * 1024

function generateChatId() {
  return 'pdf_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

const goBack = () => {
  router.push('/')
}

// 加载PDF历史会话
const loadPdfHistory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/ai/history/pdf`)
    const chatIds = await response.json()

    if (chatIds && chatIds.length > 0) {
      const sessions = []
      for (const chatId of chatIds) {
        const msgResponse = await fetch(`${API_BASE_URL}/ai/history/pdf/${encodeURIComponent(chatId)}`)
        const messages = await msgResponse.json()

        const formattedMessages = messages.map(msg => ({
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content
        }))

        sessions.push({
          id: chatId,
          title: 'PDF对话 ' + chatId.slice(-6),
          messages: formattedMessages,
          createdAt: Date.now()
        })
      }
      pdfSessions.value = sessions.reverse()
    }
  } catch (error) {
    console.error('加载PDF历史失败:', error)
  }
}

// 创建新PDF会话
const createNewPdfSession = () => {
  clearChat()
}

// 切换PDF会话
const switchPdfSession = async (sessionId) => {
  const session = pdfSessions.value.find(s => s.id === sessionId)
  if (session) {
    currentPdfId.value = sessionId
    chatId.value = sessionId
    messages.value = [...session.messages]
    uploadedFile.value = { name: session.title }
    nextTick(() => scrollToBottom())
  }
}

// 删除PDF会话
const deletePdfSession = (sessionId, event) => {
  event.stopPropagation()
  const index = pdfSessions.value.findIndex(s => s.id === sessionId)
  if (index > -1) {
    pdfSessions.value.splice(index, 1)
  }
  if (currentPdfId.value === sessionId) {
    createNewPdfSession()
  }
}

const handleFileUpload = async (event) => {
  const file = event.target.files[0]
  if (file && file.type === 'application/pdf') {
    if (file.size > MAX_PDF_SIZE) {
      alert('PDF文件不能超过50MB')
      event.target.value = ''
      return
    }

    isUploading.value = true
    uploadedFile.value = file
    chatId.value = generateChatId()
    
    try {
      // 创建FormData对象
      const formData = new FormData()
      formData.append('file', file)
      
      // 上传文件到后端，chatId放在URL路径中
      const response = await fetch(`${API_BASE_URL}/ai/pdf/upload/${encodeURIComponent(chatId.value)}`, {
        method: 'POST',
        body: formData
      })
      
      if (!response.ok) {
        if (response.status === 413) {
          throw new Error('文件过大，请上传50MB以内的PDF')
        }
        throw new Error(`上传失败，状态码：${response.status}`)
      }

      const contentType = response.headers.get('content-type') || ''
      if (!contentType.includes('application/json')) {
        throw new Error('后端返回格式异常，请检查服务日志')
      }

      const result = await response.json()
      
      if (result.success) {
        isUploading.value = false
        pdfContent.value = '文档已上传成功，正在解析中...'
        // 创建PDF预览URL
        pdfUrl.value = URL.createObjectURL(file)
        messages.value.push({
          role: 'system',
          content: `已成功上传并解析文档：${file.name}`
        })
        messages.value.push({
          role: 'assistant',
          content: '文档已加载完成！您可以向我询问关于这份文档的任何问题，我会基于文档内容为您解答。'
        })
      } else {
        isUploading.value = false
        alert('文件上传失败：' + result.message)
        uploadedFile.value = null
        chatId.value = ''
      }
    } catch (error) {
      isUploading.value = false
      console.error('上传文件失败:', error)
      alert(error.message || '上传文件失败，请检查网络连接')
      uploadedFile.value = null
      chatId.value = ''
      event.target.value = ''
    }
  } else {
    alert('请上传PDF文件')
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !uploadedFile.value || loading.value) return
  
  const userMessage = inputMessage.value.trim()
  messages.value.push({
    role: 'user',
    content: userMessage
  })
  
  inputMessage.value = ''
  loading.value = true
  
  await nextTick()
  scrollToBottom()
  
  const assistantMessage = { role: 'assistant', content: '' }
  messages.value.push(assistantMessage)
  
  try {
    const eventSource = new EventSource(
      `${API_BASE_URL}/ai/pdf/chat?prompt=${encodeURIComponent(userMessage)}&chatId=${encodeURIComponent(chatId.value)}`
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
  const messagesContainer = document.querySelector('.messages')
  if (messagesContainer) {
    messagesContainer.scrollTop = messagesContainer.scrollHeight
  }
}

const clearChat = () => {
  messages.value = []
  uploadedFile.value = null
  pdfContent.value = ''
  chatId.value = ''
  pdfUrl.value = ''
  currentPage.value = 1
  totalPages.value = 1
  currentPdfId.value = ''
}

onMounted(() => {
  loadPdfHistory()
})

const onPdfLoad = (pdf) => {
  totalPages.value = pdf.numPages
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}
</script>

<template>
  <div class="chat-pdf">
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"></path>
          </svg>
        </button>
        <span class="header-title">ChatPDF</span>
      </div>
      <div class="logo">AI Hub</div>
    </header>
    
    <div class="pdf-container">
      <!-- 历史记录侧边栏 -->
      <div v-if="pdfSessions.length > 0" class="history-sidebar">
        <div class="history-header">
          <h3>历史记录</h3>
          <button class="new-chat-btn" @click="createNewPdfSession">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
          </button>
        </div>
        <div class="history-list">
          <div 
            v-for="session in pdfSessions" 
            :key="session.id"
            :class="['history-item', { active: currentPdfId === session.id }]"
            @click="switchPdfSession(session.id)"
          >
            <div class="history-icon">📄</div>
            <div class="history-info">
              <div class="history-title">{{ session.title }}</div>
              <div class="history-count">{{ session.messages.length }} 条消息</div>
            </div>
            <button class="delete-btn" @click="deletePdfSession(session.id, $event)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
      
      <!-- 上传区域 -->
      <div v-if="!uploadedFile" class="upload-section">
        <div class="upload-card">
          <div class="upload-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
          </div>
          <h2>智能文档阅读</h2>
          <p class="subtitle">上传PDF文档，与AI进行智能问答</p>
          
          <label class="upload-btn">
            <input 
              type="file" 
              accept=".pdf" 
              @change="handleFileUpload"
              style="display: none"
            />
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            选择PDF文件
          </label>
          
          <div class="features">
            <div class="feature-item">
              <span class="feature-icon">📄</span>
              <span>支持PDF格式</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🔍</span>
              <span>智能内容提取</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">💬</span>
              <span>文档问答对话</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 聊天区域 -->
      <div v-else class="chat-section">
        <div class="pdf-sidebar">
          <div class="pdf-info">
            <div class="pdf-icon">📄</div>
            <h4>{{ uploadedFile.name }}</h4>
            <p class="pdf-size">{{ (uploadedFile.size / 1024).toFixed(1) }} KB</p>
          </div>
          
          <div class="pdf-preview">
            <h5>文档预览</h5>
            <div class="preview-content">
              <div v-if="pdfUrl" class="pdf-viewer">
                <iframe 
                  :src="pdfUrl + '#page=' + currentPage" 
                  width="100%" 
                  height="600px" 
                  style="border: none; border-radius: 8px;"
                ></iframe>
                <div class="pdf-controls">
                  <button @click="prevPage" :disabled="currentPage <= 1">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                  </button>
                  <span>{{ currentPage }} / {{ totalPages }}</span>
                  <button @click="nextPage" :disabled="currentPage >= totalPages">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                  </button>
                </div>
              </div>
              <div v-else>
                {{ pdfContent }}
              </div>
            </div>
          </div>
          
          <button class="new-file-btn" @click="clearChat">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            上传新文件
          </button>
        </div>
        
        <div class="chat-main">
          <div class="messages">
            <div 
              v-for="(msg, idx) in messages" 
              :key="idx"
              :class="['message', msg.role]"
            >
              <div v-if="msg.role === 'system'" class="system-message">
                {{ msg.content }}
              </div>
              <template v-else>
                <div class="avatar">
                  <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
                  <div class="bubble">{{ msg.content }}</div>
                </div>
              </template>
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
            <div class="input-wrapper">
              <input 
                v-model="inputMessage"
                placeholder="询问关于文档的问题..."
                @keyup.enter="sendMessage"
                :disabled="loading"
              />
              <button 
                class="send-btn" 
                :disabled="!inputMessage.trim() || loading"
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
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-pdf {
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

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
}

.pdf-container {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  gap: 20px;
}

/* 历史记录侧边栏 */
.history-sidebar {
  width: 260px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  max-height: calc(100vh - 140px);
  overflow-y: auto;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.history-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.new-chat-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #3b82f6;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #2563eb;
}

.new-chat-btn svg {
  width: 16px;
  height: 16px;
  color: white;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.history-item:hover {
  background: #f1f5f9;
}

.history-item.active {
  background: #eff6ff;
  border-color: #3b82f6;
}

.history-icon {
  font-size: 20px;
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-count {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.delete-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fee2e2;
}

.delete-btn svg {
  width: 14px;
  height: 14px;
  color: #ef4444;
}

/* 上传区域 */
.upload-section {
  width: 100%;
  max-width: 600px;
}

.upload-card {
  background: white;
  padding: 50px;
  border-radius: 24px;
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.1);
  text-align: center;
}

.upload-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 25px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon svg {
  width: 40px;
  height: 40px;
  color: white;
}

.upload-card h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #888;
  margin-bottom: 30px;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  padding: 15px 30px;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.upload-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4);
}

.upload-btn svg {
  width: 20px;
  height: 20px;
}

.features {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 30px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.feature-icon {
  font-size: 24px;
}

/* 聊天区域 */
.chat-section {
  width: 100%;
  display: flex;
  gap: 20px;
  height: calc(100vh - 80px);
}

.pdf-sidebar {
  width: 450px;
  background: white;
  border-radius: 16px;
  padding: 25px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pdf-info {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.pdf-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.pdf-info h4 {
  color: #333;
  margin-bottom: 5px;
  word-break: break-all;
}

.pdf-size {
  color: #999;
  font-size: 14px;
}

.pdf-preview {
  flex: 1;
  overflow-y: auto;
}

.pdf-preview h5 {
  color: #333;
  margin-bottom: 10px;
}

.preview-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}

.pdf-viewer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pdf-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
}

.pdf-controls button {
  width: 32px;
  height: 32px;
  border: none;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.pdf-controls button:hover:not(:disabled) {
  background: #3b82f6;
  color: white;
}

.pdf-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pdf-controls button svg {
  width: 16px;
  height: 16px;
}

.pdf-controls span {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.new-file-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.new-file-btn:hover {
  background: #f8fafc;
  border-color: #3b82f6;
  color: #3b82f6;
}

.new-file-btn svg {
  width: 18px;
  height: 18px;
}

.chat-main {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
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

.system-message {
  background: #f0f9ff;
  color: #0369a1;
  padding: 12px 16px;
  border-radius: 12px;
  text-align: center;
  font-size: 14px;
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

.content {
  max-width: 70%;
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

.input-wrapper input {
  flex: 1;
  border: none;
  outline: none;
  padding: 10px 15px;
  font-size: 14px;
  font-family: inherit;
  background: transparent;
}

.input-wrapper input::placeholder {
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

@media (max-width: 768px) {
  .chat-pdf {
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
  
  .logo {
    font-size: 18px;
  }
  
  .back-btn {
    width: 32px;
    height: 32px;
  }
  
  .back-btn svg {
    width: 18px;
    height: 18px;
  }
  
  .pdf-container {
    padding: 15px 10px;
    gap: 15px;
  }
  
  .history-sidebar {
    width: 100%;
    max-height: none;
    padding: 15px;
    margin-bottom: 10px;
  }
  
  .history-header h3 {
    font-size: 14px;
  }
  
  .new-chat-btn {
    width: 28px;
    height: 28px;
  }
  
  .new-chat-btn svg {
    width: 14px;
    height: 14px;
  }
  
  .history-item {
    padding: 10px;
  }
  
  .history-icon {
    font-size: 18px;
  }
  
  .history-title {
    font-size: 13px;
  }
  
  .history-count {
    font-size: 11px;
  }
  
  .delete-btn {
    width: 24px;
    height: 24px;
  }
  
  .delete-btn svg {
    width: 12px;
    height: 12px;
  }
  
  .upload-section {
    padding: 0;
  }
  
  .upload-card {
    padding: 30px 20px;
  }
  
  .upload-icon {
    width: 60px;
    height: 60px;
    margin-bottom: 20px;
  }
  
  .upload-icon svg {
    width: 30px;
    height: 30px;
  }
  
  .upload-card h2 {
    font-size: 22px;
  }
  
  .subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }
  
  .upload-btn {
    padding: 12px 25px;
    font-size: 14px;
  }
  
  .upload-btn svg {
    width: 18px;
    height: 18px;
  }
  
  .features {
    gap: 20px;
    margin-top: 20px;
  }
  
  .feature-item {
    font-size: 13px;
  }
  
  .feature-icon {
    font-size: 20px;
  }
  
  .chat-section {
    flex-direction: column;
    height: auto;
  }
  
  .pdf-sidebar {
    width: 100%;
    display: none;
  }
  
  .pdf-sidebar.show-mobile {
    display: flex;
    padding: 15px;
    margin-bottom: 10px;
  }
  
  .pdf-info {
    padding-bottom: 15px;
  }
  
  .pdf-icon {
    font-size: 36px;
    margin-bottom: 8px;
  }
  
  .pdf-info h4 {
    font-size: 14px;
  }
  
  .pdf-size {
    font-size: 12px;
  }
  
  .pdf-preview {
    max-height: 300px;
  }
  
  .pdf-preview h5 {
    font-size: 14px;
    margin-bottom: 8px;
  }
  
  .preview-content {
    font-size: 13px;
    max-height: 250px;
  }
  
  .pdf-viewer iframe {
    height: 400px;
  }
  
  .pdf-controls {
    padding: 8px;
    gap: 10px;
  }
  
  .pdf-controls button {
    width: 28px;
    height: 28px;
  }
  
  .pdf-controls button svg {
    width: 14px;
    height: 14px;
  }
  
  .pdf-controls span {
    font-size: 13px;
  }
  
  .new-file-btn {
    padding: 10px;
    font-size: 14px;
  }
  
  .new-file-btn svg {
    width: 16px;
    height: 16px;
  }
  
  .chat-main {
    border-radius: 12px;
  }
  
  .messages {
    padding: 15px;
  }
  
  .message {
    gap: 8px;
  }
  
  .system-message {
    padding: 10px 14px;
    font-size: 13px;
  }
  
  .avatar {
    width: 32px;
    height: 32px;
  }
  
  .avatar svg {
    width: 16px;
    height: 16px;
  }
  
  .content {
    max-width: calc(100% - 60px);
  }
  
  .bubble {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .input-area {
    padding: 15px;
  }
  
  .input-wrapper {
    padding: 6px;
  }
  
  .input-wrapper input {
    padding: 8px 12px;
    font-size: 14px;
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
  
  .pdf-container {
    padding: 10px 8px;
  }
  
  .upload-card {
    padding: 25px 15px;
  }
  
  .upload-card h2 {
    font-size: 20px;
  }
  
  .subtitle {
    font-size: 13px;
  }
  
  .upload-btn {
    padding: 10px 20px;
    font-size: 13px;
  }
  
  .features {
    flex-direction: column;
    gap: 15px;
  }
  
  .feature-item {
    flex-direction: row;
    gap: 8px;
  }
  
  .pdf-viewer iframe {
    height: 300px;
  }
  
  .message-content {
    max-width: calc(100% - 50px);
  }
  
  .bubble {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .input-area {
    padding: 12px;
  }
}
</style>
