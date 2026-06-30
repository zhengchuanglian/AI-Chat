<script setup>
</script>

<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 移动端适配样式 */
@media (max-width: 768px) {
  .ai-chat {
    flex-direction: column;
  }
  
  .sidebar {
    position: absolute;
    width: 100%;
    z-index: 100;
    height: auto;
    min-height: auto;
    transform: translateY(-100%);
    transition: transform 0.3s ease;
  }
  
  .sidebar.active {
    transform: translateY(0);
  }
  
  .sidebar-header,
  .chat-list,
  .sidebar-footer {
    display: flex;
    flex-wrap: wrap;
  }
  
  .chat-item {
    flex: 1 0 45%;
    margin: 2.5%;
  }
  
  .main-content {
    width: 100%;
  }
  
  .header {
    padding: 10px 15px;
  }
  
  .header-title {
    font-size: 14px;
  }
  
  .chat-container {
    padding: 0 10px 10px;
  }
  
  .message {
    gap: 8px;
  }
  
  .content {
    max-width: calc(100% - 60px);
  }
  
  .bubble {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .input-wrapper {
    padding: 6px;
  }
  
  .input-wrapper textarea {
    padding: 8px 10px;
    font-size: 14px;
  }
  
  .attach-btn, .send-btn {
    width: 36px;
    height: 36px;
  }
  
  .avatar {
    width: 30px;
    height: 30px;
  }
  
  .avatar svg {
    width: 16px;
    height: 16px;
  }
  
  .score-display {
    top: 5px;
    right: 5px;
    font-size: 12px;
    padding: 2px 6px;
  }
}

@media (max-width: 480px) {
  .sidebar {
    width: 100%;
    height: 300px;
  }
  
  .chat-item {
    flex: 1 0 100%;
    margin: 2% 0;
  }
  
  .content {
    max-width: calc(100% - 50px);
  }
  
  .message.assistant .bubble {
    border-top-left-radius: 12px;
  }
  
  .message.user .bubble {
    border-top-right-radius: 12px;
  }
  
  .header-title {
    font-size: 16px;
  }
  
  .logo {
    font-size: 16px;
  }
  
  .file-preview-area {
    padding: 8px 0 16px 0;
  }
  
  .file-tag {
    font-size: 12px;
    padding: 4px 8px;
  }
}
</style>