<!-- 仪表盘页面 -->
<template>
  <div class="dashboard">
    欢迎来到医学影像分析系统，请选择左侧菜单进行操作。
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Upload, List } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 当前日期
const currentDate = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})


</script>

<style scoped>
.dashboard {
  padding: 20px;
}

</style> 