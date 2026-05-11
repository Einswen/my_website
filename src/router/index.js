import { createRouter, createWebHistory } from 'vue-router'
import ForecastView from '../views/ForecastView.vue'
import HomeView from '../views/HomeView.vue'
import MessageBoardView from '../views/MessageBoardView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/forecast',
      name: 'forecast',
      component: ForecastView,
    },
    {
      path: '/messages',
      name: 'messages',
      component: MessageBoardView,
    },
  ],
})

export default router
