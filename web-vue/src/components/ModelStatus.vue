<template>
  <el-tag :type="statusType" :effect="tagEffect">
    {{ statusText }}
    <el-icon v-if="status === 1" class="rotating">
      <Loading />
    </el-icon>
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

// 模型状态常量
const STATUS_UNTRAINED = 0
const STATUS_TRAINING = 1
const STATUS_TRAINED_SUCCESS = 2
const STATUS_TRAINED_FAILED = 3
const STATUS_PUBLISHED = 4

// 组件属性
const props = defineProps({
  status: {
    type: Number,
    required: true
  },
  effect: {
    type: String as () => 'light' | 'dark' | 'plain',
    default: 'light'
  }
})

// 根据状态计算显示文本
const statusText = computed(() => {
  switch (props.status) {
    case STATUS_UNTRAINED: return '未训练'
    case STATUS_TRAINING: return '训练中'
    case STATUS_TRAINED_SUCCESS: return '训练成功'
    case STATUS_TRAINED_FAILED: return '训练失败'
    case STATUS_PUBLISHED: return '已发布'
    default: return '未知状态'
  }
})

// 根据状态计算Tag类型
const statusType = computed(() => {
  switch (props.status) {
    case STATUS_UNTRAINED: return 'info'
    case STATUS_TRAINING: return 'warning'
    case STATUS_TRAINED_SUCCESS: return 'success'
    case STATUS_TRAINED_FAILED: return 'danger'
    case STATUS_PUBLISHED: return 'success'
    default: return 'info'
  }
})

// 标签样式
const tagEffect = computed(() => props.effect)
</script>

<style scoped>
.rotating {
  animation: rotate 1.5s linear infinite;
  margin-left: 5px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style> 