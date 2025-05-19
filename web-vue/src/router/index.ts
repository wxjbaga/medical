import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'


const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '首页',
          requiresAuth: true
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: {
          title: '个人中心',
          requiresAuth: true
        }
      },
      {
        path: 'heart/image-analysis',
        name: 'HeartImageAnalysis',
        component: () => import('@/views/HeartImageAnalysis.vue'),
        meta: {
          title: '医学图像分析',
          requiresAuth: true
        }
      },
      {
        path: 'model/detail/:id',
        name: 'ModelDetail',
        component: () => import('@/views/model/detail.vue'),
        meta: {
          title: '模型详情',
          requiresAuth: true
        }
      },
      {
        path: 'model/evaluation',
        name: 'ModelEvaluation',
        component: () => import('@/views/model/evaluation.vue'),
        meta: {
          title: '模型评估',
          requiresAuth: true
        }
      },
      {
        path: 'model/segmentation',
        name: 'ModelSegmentation',
        component: () => import('@/views/model/segmentation.vue'),
        meta: {
          title: '影像分割',
          requiresAuth: true
        }
      },
      {
        path: 'model/history',
        name: 'OperationHistory',
        component: () => import('@/views/model/HistoryList.vue'),
        meta: {
          title: '操作历史',
          requiresAuth: true
        }
      },
      {
        path: 'model/feedback',
        name: 'ModelFeedback',
        component: () => import('@/views/model/FeedbackList.vue'),
        meta: {
          title: '评估反馈',
          requiresAuth: true
        }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/components/Layout.vue'),
    meta: {
      requiresAuth: true,
      requiresAdmin: true
    },
    children: [
      {
        path: '',
        name: 'Admin',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: {
          title: '管理控制台',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'datasets',
        name: 'DatasetManagement',
        component: () => import('@/views/dataset/index.vue'),
        meta: {
          title: '数据集管理',
          requiresAuth: true
        }
      },
      {
        path: 'model',
        name: 'Model',
        component: () => import('@/views/model/index.vue'),
        meta: {
          title: '模型管理',
          requiresAuth: true
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)

  // 设置页面标题
  document.title = `${to.meta.title} - 医学影像分析系统`

  if (requiresAuth) {
    if (!userStore.isLoggedIn()) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    if (requiresAdmin && !userStore.isAdmin()) {
      next({ name: 'Dashboard' })
      return
    }
  }

  next()
})

export default router 