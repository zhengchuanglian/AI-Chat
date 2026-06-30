<script setup>
import { ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentScenario = ref(0)
const inputMessage = ref('')
const messages = ref([])
const gameStarted = ref(false)
const score = ref(0)
const showResult = ref(false)
const loading = ref(false)
const chatId = ref('')
const mood = ref('生气') // 当前心情状态：生气、委屈、缓和、开心
const moodScore = ref(0) // 心情得分 0-100

const moodConfig = {
  '生气': { emoji: '😠', color: '#ff4444', minScore: 0, maxScore: 25 },
  '委屈': { emoji: '😢', color: '#ff8800', minScore: 26, maxScore: 50 },
  '缓和': { emoji: '😌', color: '#ffcc00', minScore: 51, maxScore: 75 },
  '开心': { emoji: '😊', color: '#44ff44', minScore: 76, maxScore: 100 }
}

const currentMood = computed(() => {
  for (const [moodName, config] of Object.entries(moodConfig)) {
    if (moodScore.value >= config.minScore && moodScore.value <= config.maxScore) {
      return { name: moodName, ...config }
    }
  }
  return { name: '生气', ...moodConfig['生气'] }
})

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const scenarios = [
  {
    id: 1,
    title: '女朋友生气了',
    description: '你因为加班忘记了和女朋友的约会，她非常生气。你会怎么哄她？',
    context: '你又忘了我们的约会！你总是这样，工作比我重要是吗？',
    hints: ['承认错误', '表达歉意', '提出补偿']
  },
  {
    id: 2,
    title: '纪念日忘了',
    description: '今天是你们的恋爱纪念日，但你完全忘记了。',
    context: '今天是什么日子你记得吗？（委屈地看着你）',
    hints: ['立刻想起', '真诚道歉', '准备惊喜']
  },
  {
    id: 3,
    title: '玩游戏被发现了',
    description: '你答应陪她逛街，结果偷偷在家玩游戏被她发现了。',
    context: '你不是说要陪我的吗？游戏就那么好玩？',
    hints: ['放下游戏', '诚恳认错', '主动陪伴']
  }
]

function generateChatId() {
  return 'game_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

const goBack = () => {
  router.push('/')
}

const startGame = () => {
  gameStarted.value = true
  chatId.value = generateChatId()
  score.value = 0
  moodScore.value = 0
  mood.value = '生气'
  messages.value = [{
    role: 'girlfriend',
    content: scenarios[currentScenario.value].context
  }]
  // 发送场景上下文给AI
  sendToAI(`场景：${scenarios[currentScenario.value].title}\n${scenarios[currentScenario.value].description}\n\n女朋友说："${scenarios[currentScenario.value].context}"\n\n请扮演生气的女朋友回复，要求：语气要委屈、生气，但也要给机会让男朋友哄。只回复女朋友说的话，不要加任何前缀。`, true)
}

const sendToAI = async (prompt, isInit = false, playerQuality = 'neutral') => {
  loading.value = true
  
  try {
    const eventSource = new EventSource(
      `${API_BASE_URL}/ai/game?prompt=${encodeURIComponent(prompt)}&chatId=${encodeURIComponent(chatId.value)}`
    )
    
    let aiResponse = ''
    
    let hasReceivedData = false
    
    eventSource.onmessage = (event) => {
      hasReceivedData = true
      // 检查是否是关闭标记
      if (event.data === '[CLOSE]') {
        eventSource.close()
        loading.value = false
        
        if (!isInit && aiResponse) {
          messages.value.push({
            role: 'girlfriend',
            content: aiResponse
          })
          
          // 根据玩家回复质量评分
          let points = 0
          
          if (playerQuality === 'good') {
            // 玩家回复得好，大幅加分
            points = 20
            moodScore.value = Math.min(moodScore.value + 20, 100)
          } else if (playerQuality === 'bad') {
            // 玩家回复得不好，减分
            points = -15
            moodScore.value = Math.max(moodScore.value - 15, 0)
          } else {
            // 一般回复，小幅加分
            points = 5
            moodScore.value = Math.min(moodScore.value + 5, 100)
          }
          
          // 同时根据AI回复内容调整
          const content = aiResponse.toLowerCase()
          if (content.includes('原谅') || content.includes('开心') || content.includes('爱你')) {
            points += 10
            moodScore.value = Math.min(moodScore.value + 10, 100)
          } else if (content.includes('哼') || content.includes('不理') || content.includes('生气')) {
            points -= 5
            moodScore.value = Math.max(moodScore.value - 5, 0)
          }
          
          score.value = Math.max(0, Math.min(100, score.value + points))
          
          // 更新心情状态
          if (moodScore.value >= 76) {
            mood.value = '开心'
          } else if (moodScore.value >= 51) {
            mood.value = '缓和'
          } else if (moodScore.value >= 26) {
            mood.value = '委屈'
          } else {
            mood.value = '生气'
          }
          
          // 检查是否通关（达到100分）
          if (score.value >= 100) {
            setTimeout(() => {
              showResult.value = true
            }, 1000)
          }
        }
        
        nextTick(() => {
          const chatArea = document.querySelector('.messages')
          if (chatArea) chatArea.scrollTop = chatArea.scrollHeight
        })
        return
      }
      aiResponse += event.data
    }
    
    eventSource.onerror = (error) => {
      eventSource.close()
      loading.value = false
      // 如果已经收到数据，说明是正常结束，不显示错误消息
      if (!isInit && !hasReceivedData) {
        messages.value.push({
          role: 'girlfriend',
          content: '哼，我不想理你了！'
        })
      }
    }
  } catch (error) {
    loading.value = false
    if (!isInit) {
      messages.value.push({
        role: 'girlfriend',
        content: '哼，我不想理你了！'
      })
    }
  }
}

// 判断玩家回复质量的函数
const evaluatePlayerResponse = (response) => {
  const content = response.toLowerCase()
  
  // 定义关键词
  const goodKeywords = ['对不起', '抱歉', '我错了', '原谅我', '补偿', '赔罪', '请你', '爱你', '想你', '礼物', '惊喜', '陪你', '陪你逛街', '陪你吃饭', '看电影', '买']
  const badKeywords = ['烦不烦', '神经病', '有病', '滚', '闭嘴', '别闹', '至于', '小题大做', '无理取闹', '别生气', '不就是', '至于吗']
  const neutralKeywords = ['别生气', '好了', '算了', '行了', '知道了']
  
  let goodCount = 0
  let badCount = 0
  
  goodKeywords.forEach(keyword => {
    if (content.includes(keyword)) goodCount++
  })
  
  badKeywords.forEach(keyword => {
    if (content.includes(keyword)) badCount++
  })
  
  if (badCount > 0) {
    return 'bad'
  } else if (goodCount >= 2) {
    return 'good'
  } else if (goodCount === 1) {
    return 'neutral'
  } else {
    return 'neutral'
  }
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  const userMsg = inputMessage.value.trim()
  messages.value.push({
    role: 'user',
    content: userMsg
  })
  
  // 评估玩家回复质量
  const quality = evaluatePlayerResponse(userMsg)
  
  inputMessage.value = ''
  
  // 发送给AI，附带玩家回复质量信息
  const promptWithQuality = `[玩家回复质量：${quality}]\n${userMsg}`
  sendToAI(promptWithQuality, false, quality)
}

const nextScenario = () => {
  if (currentScenario.value < scenarios.length - 1) {
    currentScenario.value++
    messages.value = [{
      role: 'girlfriend',
      content: scenarios[currentScenario.value].context
    }]
    // 发送新场景给AI
    sendToAI(`切换到新场景：${scenarios[currentScenario.value].title}\n${scenarios[currentScenario.value].description}\n\n女朋友说："${scenarios[currentScenario.value].context}"\n\n请扮演生气的女朋友回复。只回复女朋友说的话。`, true)
  } else {
    showResult.value = true
  }
}

const restartGame = () => {
  currentScenario.value = 0
  messages.value = []
  gameStarted.value = false
  score.value = 0
  showResult.value = false
  chatId.value = ''
}
</script>

<template>
  <div class="comfort-simulator">
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"></path>
          </svg>
        </button>
        <span class="header-title">哄哄模拟器</span>
      </div>
      <div class="logo">AI Hub</div>
    </header>
    
    <div class="game-container">
      <!-- 开始界面 -->
      <div v-if="!gameStarted" class="start-screen">
        <div class="heart-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </div>
        <h2>哄哄模拟器</h2>
        <p class="subtitle">一个帮助你练习哄女朋友开心的聊天小游戏</p>
        <div class="game-info">
          <p>🎮 游戏说明：</p>
          <ul>
            <li>面对不同的场景，选择合适的回复</li>
            <li>哄得女朋友开心可以获得分数</li>
            <li>完成所有场景挑战，成为哄人高手</li>
          </ul>
        </div>
        <button class="start-btn" @click="startGame">开始游戏</button>
      </div>
      
      <!-- 游戏界面 -->
      <div v-else-if="!showResult" class="game-screen">
        <div class="scenario-info">
          <div class="progress">场景 {{ currentScenario + 1 }} / {{ scenarios.length }}</div>
          <div class="mood-status" :style="{ color: currentMood.color }">
            <span class="mood-emoji">{{ currentMood.emoji }}</span>
            <span class="mood-text">{{ currentMood.name }}</span>
            <div class="mood-bar">
              <div class="mood-fill" :style="{ width: moodScore + '%', background: currentMood.color }"></div>
            </div>
            <span class="mood-score">{{ moodScore }}/100</span>
          </div>
          <div class="score">得分: {{ score }}</div>
        </div>
        
        <div class="scenario-card">
          <h3>{{ scenarios[currentScenario].title }}</h3>
          <p class="scenario-desc">{{ scenarios[currentScenario].description }}</p>
          <div class="hints">
            <span class="hint-tag" v-for="(hint, idx) in scenarios[currentScenario].hints" :key="idx">
              💡 {{ hint }}
            </span>
          </div>
        </div>
        
        <div class="chat-area">
          <div class="messages">
            <div 
              v-for="(msg, idx) in messages" 
              :key="idx"
              :class="['message', msg.role]"
            >
              <div class="avatar">
                <span v-if="msg.role === 'girlfriend'">👩</span>
                <span v-else>👨</span>
              </div>
              <div class="bubble">{{ msg.content }}</div>
            </div>
            <div v-if="loading" class="message girlfriend">
              <div class="avatar">👩</div>
              <div class="bubble loading">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
          
          <div class="input-area">
            <input 
              v-model="inputMessage" 
              placeholder="输入你想说的话..."
              @keyup.enter="sendMessage"
              :disabled="loading"
            />
            <button @click="sendMessage" :disabled="loading || !inputMessage.trim()">发送</button>
          </div>
        </div>
        
        <button class="next-btn" @click="nextScenario">
          {{ currentScenario < scenarios.length - 1 ? '下一个场景 →' : '查看结果' }}
        </button>
      </div>
      
      <!-- 结果界面 -->
      <div v-else class="result-screen">
        <div class="result-icon">🎉</div>
        <h2>游戏结束</h2>
        <div class="final-score">
          <span class="score-label">最终得分</span>
          <span class="score-value">{{ score }}</span>
        </div>
        <div class="result-message">
          <p v-if="score >= 150">🏆 哄人大师！你太会哄人了！</p>
          <p v-else-if="score >= 100">👍 表现不错，继续加油！</p>
          <p v-else>💪 还需要多练习哦~</p>
        </div>
        <button class="restart-btn" @click="restartGame">再玩一次</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.game-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #ffeef5 0%, #e6f4ff 100%);
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
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

.game-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #ff6b8b;
}

.game-area {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.scenario-selector {
  background: white;
  border-radius: 16px;
  padding: 25px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.scenario-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  text-align: center;
}

.scenario-description {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  text-align: center;
  margin-bottom: 20px;
}

.scenario-hints {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hint-tag {
  background: #fff0f5;
  color: #ff6b8b;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
}

.scenario-options {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.scenario-btn {
  padding: 10px 20px;
  border: 1px solid #ff6b8b;
  background: white;
  color: #ff6b8b;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.scenario-btn.active {
  background: #ff6b8b;
  color: white;
}

.start-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #ff6b8b 0%, #ff8e53 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 20px;
  transition: all 0.2s;
}

.start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 139, 0.3);
}

.game-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  background: white;
  border-radius: 16px;
  padding: 15px 25px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.mood-display {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mood-emoji {
  font-size: 28px;
  margin-bottom: 5px;
}

.mood-text {
  font-size: 14px;
  font-weight: 600;
}

.chat-container {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
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

.message.user {
  flex-direction: row-reverse;
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

.message.girlfriend .avatar {
  background: linear-gradient(135deg, #ff6b8b 0%, #ff8e53 100%);
  color: white;
}

.message.user .avatar {
  background: #f0f9ff;
  color: #2563eb;
}

.avatar svg {
  width: 20px;
  height: 20px;
}

.message-content {
  max-width: 70%;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message.girlfriend .bubble {
  background: #fff8f9;
  color: #333;
  border: 1px solid #ffebee;
  border-top-left-radius: 4px;
}

.message.user .bubble {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
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
  background: linear-gradient(135deg, #ff6b8b 0%, #ff8e53 100%);
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
  box-shadow: 0 4px 12px rgba(255, 107, 139, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.result-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.result-content {
  background: white;
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  max-width: 90%;
  width: 400px;
}

.result-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.result-message {
  font-size: 16px;
  color: #666;
  margin-bottom: 25px;
  line-height: 1.6;
}

.result-score {
  font-size: 20px;
  font-weight: 600;
  color: #ff6b8b;
  margin-bottom: 25px;
}

.restart-btn {
  padding: 12px 30px;
  background: linear-gradient(135deg, #ff6b8b 0%, #ff8e53 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.restart-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 139, 0.3);
}

@media (max-width: 768px) {
  .comfort-simulator {
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
  
  .game-title {
    font-size: 16px;
  }
  
  .logo {
    font-size: 18px;
  }
  
  .game-area {
    padding: 15px 10px;
  }
  
  .scenario-selector {
    padding: 20px 15px;
  }
  
  .scenario-title {
    font-size: 18px;
  }
  
  .scenario-description {
    font-size: 14px;
  }
  
  .scenario-hints {
    gap: 8px;
  }
  
  .hint-tag {
    font-size: 12px;
    padding: 4px 10px;
  }
  
  .scenario-options {
    gap: 10px;
    margin-top: 15px;
  }
  
  .scenario-btn {
    padding: 8px 15px;
    font-size: 13px;
  }
  
  .start-btn {
    padding: 12px;
    font-size: 15px;
    margin-top: 15px;
  }
  
  .status-bar {
    padding: 12px 15px;
  }
  
  .stat-item {
    min-width: 60px;
  }
  
  .stat-label {
    font-size: 12px;
  }
  
  .stat-value {
    font-size: 16px;
  }
  
  .mood-emoji {
    font-size: 24px;
  }
  
  .mood-text {
    font-size: 13px;
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
  
  .avatar {
    width: 32px;
    height: 32px;
  }
  
  .avatar svg {
    width: 16px;
    height: 16px;
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
  
  .result-content {
    padding: 30px 20px;
    width: 90%;
  }
  
  .result-title {
    font-size: 20px;
  }
  
  .result-message {
    font-size: 14px;
  }
  
  .result-score {
    font-size: 18px;
  }
  
  .restart-btn {
    padding: 10px 25px;
    font-size: 15px;
  }
  
  .game-screen {
    padding: 10px;
  }
  
  .scenario-info {
    flex-direction: column;
    gap: 10px;
    padding: 10px;
  }
  
  .mood-status {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .mood-bar {
    flex: 1;
  }
  
  .scenario-card {
    padding: 15px;
    margin: 10px 0;
  }
  
  .scenario-card h3 {
    font-size: 16px;
    margin-bottom: 8px;
  }
  
  .scenario-desc {
    font-size: 13px;
    margin-bottom: 10px;
  }
  
  .hints {
    flex-wrap: wrap;
  }
  
  .chat-area {
    height: 400px;
  }
  
  .next-btn {
    padding: 12px 20px;
    font-size: 14px;
    width: 100%;
    margin-top: 10px;
  }
  
  .start-screen {
    padding: 20px;
  }
  
  .heart-icon {
    width: 60px;
    height: 60px;
    margin: 0 auto 15px;
  }
  
  .start-screen h2 {
    font-size: 22px;
    margin-bottom: 10px;
  }
  
  .subtitle {
    font-size: 14px;
    margin-bottom: 15px;
  }
  
  .game-info {
    padding: 15px;
    margin-bottom: 15px;
  }
  
  .game-info p {
    font-size: 14px;
  }
  
  .game-info li {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .message-content {
    max-width: calc(100% - 50px);
  }
  
  .message.girlfriend .bubble {
    border-top-left-radius: 12px;
  }
  
  .message.user .bubble {
    border-top-right-radius: 12px;
  }
  
  .stat-item {
    min-width: 50px;
  }
  
  .scenario-options {
    flex-direction: column;
  }
  
  .scenario-btn {
    width: 100%;
  }
  
  .back-btn {
    width: 32px;
    height: 32px;
  }
  
  .back-btn svg {
    width: 18px;
    height: 18px;
  }
  
  .chat-area {
    height: 350px;
  }
  
  .bubble {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .scenario-card {
    padding: 12px;
  }
  
  .scenario-card h3 {
    font-size: 15px;
  }
  
  .scenario-desc {
    font-size: 12px;
  }
  
  .hint-tag {
    font-size: 11px;
    padding: 3px 8px;
  }
  
  .input-area input {
    padding: 10px 12px;
    font-size: 13px;
  }
  
  .input-area button {
    padding: 10px 15px;
    font-size: 13px;
  }
  
  .result-content {
    padding: 25px 15px;
  }
  
  .result-title {
    font-size: 18px;
  }
  
  .result-score {
    font-size: 16px;
  }
  
  .restart-btn {
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>
