import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import AIChat from '../views/AIChat.vue'
import ComfortSimulator from '../views/ComfortSimulator.vue'
import SmartCustomerService from '../views/SmartCustomerService.vue'
import ChatPDF from '../views/ChatPDF.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/ai-chat',
    name: 'AIChat',
    component: AIChat
  },
  {
    path: '/comfort-simulator',
    name: 'ComfortSimulator',
    component: ComfortSimulator
  },
  {
    path: '/smart-customer-service',
    name: 'SmartCustomerService',
    component: SmartCustomerService
  },
  {
    path: '/chat-pdf',
    name: 'ChatPDF',
    component: ChatPDF
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
