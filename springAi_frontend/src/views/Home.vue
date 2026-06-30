<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const modules = [
  {
    id: 'ai-chat',
    title: 'AI 聊天',
    description: '与智能AI助手进行自然对话',
    icon: 'chat',
    route: '/ai-chat'
  },
  {
    id: 'comfort-simulator',
    title: '哄哄模拟器',
    description: '一个帮助你练习哄女朋友开心的聊天小游戏',
    icon: 'heart',
    route: '/comfort-simulator'
  },
  {
    id: 'smart-customer-service',
    title: '智能客服',
    description: '24小时在线的智能服务助手',
    icon: 'service',
    route: '/smart-customer-service'
  },
  {
    id: 'chat-pdf',
    title: 'ChatPDF',
    description: '智能文档阅读与问答系统',
    icon: 'pdf',
    route: '/chat-pdf'
  }
]

const navigateTo = (route) => {
  router.push(route)
}
</script>

<template>
  <div class="home">
    <header class="header">
      <div class="logo">AI Hub</div>
      <div class="theme-toggle">
        <svg class="moon-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
        </svg>
      </div>
    </header>
    
    <main class="main-content">
      <h1 class="page-title">AI 应用中心</h1>
      
      <div class="modules-grid">
        <div 
          v-for="module in modules" 
          :key="module.id"
          class="module-card"
          @click="navigateTo(module.route)"
        >
          <div class="module-icon">
            <!-- AI Chat Icon -->
            <svg v-if="module.icon === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 0 1-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path>
            </svg>
            <!-- Heart Icon -->
            <svg v-else-if="module.icon === 'heart'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
            </svg>
            <!-- Service Icon -->
            <svg v-else-if="module.icon === 'service'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
            <!-- PDF Icon -->
            <svg v-else-if="module.icon === 'pdf'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
          </div>
          <h3 class="module-title">{{ module.title }}</h3>
          <p class="module-description">{{ module.description }}</p>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  min-height: 100dvh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4edf5 100%);
  overflow-x: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.logo {
  font-size: 24px;
  font-weight: 700;
  color: #00bcd4;
  letter-spacing: -0.5px;
}

.theme-toggle {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.theme-toggle:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.moon-icon {
  width: 22px;
  height: 22px;
  color: #666;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

.page-title {
  text-align: center;
  font-size: 36px;
  font-weight: 600;
  color: #00bcd4;
  margin-bottom: 50px;
  letter-spacing: 2px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 25px;
  padding: 0 20px;
}

.module-card {
  background: white;
  border-radius: 16px;
  padding: 40px 25px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  -webkit-tap-highlight-color: transparent;
}

.module-card:active {
  transform: scale(0.98);
}

.module-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 188, 212, 0.15);
  border-color: rgba(0, 188, 212, 0.2);
}

.module-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 20px;
  color: #00bcd4;
}

.module-icon svg {
  width: 100%;
  height: 100%;
}

.module-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.module-description {
  font-size: 14px;
  color: #888;
  line-height: 1.6;
}

@media (max-width: 1024px) {
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header {
    padding: 15px 20px;
  }
  
  .logo {
    font-size: 20px;
  }
  
  .main-content {
    padding: 40px 15px;
  }
  
  .page-title {
    font-size: 30px;
    margin-bottom: 30px;
  }
}

@media (max-width: 768px) {
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
    padding: 0 10px;
  }
  
  .module-card {
    padding: 25px 15px;
  }
  
  .module-icon {
    width: 45px;
    height: 45px;
    margin-bottom: 15px;
  }
  
  .module-title {
    font-size: 16px;
    margin-bottom: 8px;
  }
  
  .module-description {
    font-size: 12px;
    line-height: 1.5;
  }
  
  .page-title {
    font-size: 26px;
    margin-bottom: 25px;
  }
  
  .main-content {
    padding: 30px 10px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 12px 15px;
  }
  
  .logo {
    font-size: 18px;
  }
  
  .theme-toggle {
    width: 36px;
    height: 36px;
  }
  
  .moon-icon {
    width: 20px;
    height: 20px;
  }
  
  .main-content {
    padding: 20px 10px;
  }
  
  .page-title {
    font-size: 22px;
    margin-bottom: 20px;
    letter-spacing: 1px;
  }
  
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0 8px;
  }
  
  .module-card {
    padding: 20px 12px;
    border-radius: 12px;
  }
  
  .module-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 12px;
  }
  
  .module-title {
    font-size: 14px;
    margin-bottom: 6px;
  }
  
  .module-description {
    font-size: 11px;
    line-height: 1.4;
  }
}

@media (max-width: 360px) {
  .modules-grid {
    gap: 10px;
    padding: 0 6px;
  }
  
  .module-card {
    padding: 18px 10px;
  }
  
  .module-icon {
    width: 36px;
    height: 36px;
    margin-bottom: 10px;
  }
  
  .module-title {
    font-size: 13px;
  }
  
  .module-description {
    font-size: 10px;
  }
  
  .page-title {
    font-size: 20px;
  }
}
</style>
