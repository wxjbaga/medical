<template>
  <div class="operation-history-container">
    <div class="filter-container">
      <el-form :inline="true" :model="queryParams" class="demo-form-inline">
        <el-form-item label="模型">
          <el-select v-model="queryParams.modelId" placeholder="请选择模型" clearable style="width: 200px;">
            <el-option 
              v-for="model in modelOptions" 
              :key="model.id" 
              :label="`${model.name} (创建人: ${model.createUsername})`" 
              :value="model.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作用户" v-if="isAdmin">
          <el-input
            v-model="queryParams.createUsername"
            placeholder="请输入用户名"
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="loading" :data="historyList" border style="width: 100%" row-key="id">
      <el-table-column prop="modelName" label="模型名称" min-width="120" />
      <el-table-column label="操作类型" width="100">
        <template #default="scope">
          {{ scope.row.operationType === 1 ? '评估' : '分割' }}
        </template>
      </el-table-column>
      <el-table-column label="相关图片" min-width="300">
        <template #default="scope">
          <div class="table-images">
            <div class="image-item" v-if="scope.row.originalImageUrl">
              <el-image 
                :src="scope.row.originalImageUrl" 
                fit="cover"
                :preview-src-list="[scope.row.originalImageUrl]"
                :preview-teleported="true"
                class="thumbnail"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span class="image-label">原始图片</span>
            </div>
            <div class="image-item" v-if="scope.row.resultImageUrl">
              <el-image 
                :src="scope.row.resultImageUrl" 
                fit="cover"
                :preview-src-list="[scope.row.resultImageUrl]"
                :preview-teleported="true"
                class="thumbnail"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span class="image-label">{{ scope.row.operationType === 2 ? '分割掩码图' : '标签图片' }}</span>
            </div>
            <div class="image-item" v-if="scope.row.overlayImageUrl">
              <el-image 
                :src="scope.row.overlayImageUrl" 
                fit="cover"
                :preview-src-list="[scope.row.overlayImageUrl]"
                :preview-teleported="true"
                class="thumbnail"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span class="image-label">叠加结果图</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createUserName" label="操作用户" width="120" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="handleDetail(scope.row)">详情</el-button>
          <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog title="操作历史详情" v-model="dialogVisible" width="800px">
      <div class="detail-content">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h3 class="section-title">基本信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模型名称">{{ detailData.modelName }}</el-descriptions-item>
            <el-descriptions-item label="操作类型">
              <el-tag :type="detailData.operationType === 1 ? 'success' : 'warning'">
                {{ detailData.operationType === 1 ? '评估操作' : '分割操作' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="操作人员">
              <el-tag type="info">{{ detailData.createUserName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="操作时间">{{ detailData.createTime }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 图片展示 -->
        <div class="detail-section">
          <h3 class="section-title">相关图片</h3>
          <div class="detail-images">
            <div class="detail-image-item" v-if="detailData.originalImageUrl">
              <h4>原始图片</h4>
              <el-image 
                :src="detailData.originalImageUrl" 
                fit="contain" 
                :preview-src-list="[detailData.originalImageUrl]"
                :preview-teleported="true"
              />
            </div>
            <div class="detail-image-item" v-if="detailData.resultImageUrl">
              <h4>{{ detailData.operationType === 2 ? '分割掩码图' : '标签图片' }}</h4>
              <el-image 
                :src="detailData.resultImageUrl" 
                fit="contain" 
                :preview-src-list="[detailData.resultImageUrl]"
                :preview-teleported="true"
              />
            </div>
            <div class="detail-image-item" v-if="detailData.overlayImageUrl">
              <h4>叠加结果图</h4>
              <el-image 
                :src="detailData.overlayImageUrl" 
                fit="contain" 
                :preview-src-list="[detailData.overlayImageUrl]"
                :preview-teleported="true"
              />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { pageHistories, deleteHistory, getHistoryById, type OperationHistoryVO, type HistoryQueryParams } from '@/api/history'
import { getModelList } from '@/api/model'
import { useUserStore } from '@/stores/user'

// 用户store
const userStore = useUserStore()

// 是否是管理员
const isAdmin = computed(() => userStore.isAdmin())

// 查询参数
const queryParams = reactive<HistoryQueryParams>({
  current: 1,
  size: 10,
  modelId: undefined,
  createUsername: undefined
})

// 列表数据
const historyList = ref<OperationHistoryVO[]>([])
const total = ref(0)
const loading = ref(false)
const modelOptions = ref<any[]>([])

// 详情弹窗
const dialogVisible = ref(false)
const detailData = ref<OperationHistoryVO>({} as OperationHistoryVO)

// 获取操作历史列表
const getHistoryList = async () => {
  loading.value = true
  try {
    const res = await pageHistories(queryParams)
    historyList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error('获取操作历史列表失败', error)
    ElMessage.error('获取操作历史列表失败')
  } finally {
    loading.value = false
  }
}

// 获取模型列表
const getModels = async () => {
  try {
    const res = await getModelList({ 
      current: 1, 
      size: 1000,
      status: 4 // 只获取已发布的模型
    })
    modelOptions.value = res.records
  } catch (error) {
    console.error('获取模型列表失败', error)
  }
}

// 查询按钮
const handleQuery = () => {
  queryParams.current = 1
  getHistoryList()
}

// 重置按钮
const resetQuery = () => {
  queryParams.modelId = undefined
  queryParams.createUsername = undefined
  handleQuery()
}

// 处理页码变化
const handleCurrentChange = (val: number) => {
  queryParams.current = val
  getHistoryList()
}

// 处理每页数量变化
const handleSizeChange = (val: number) => {
  queryParams.size = val
  queryParams.current = 1
  getHistoryList()
}

// 查看详情
const handleDetail = async (row: OperationHistoryVO) => {
  try {
    detailData.value = await getHistoryById(row.id)
    dialogVisible.value = true
  } catch (error) {
    console.error('获取操作历史详情失败', error)
    ElMessage.error('获取操作历史详情失败')
  }
}

// 删除操作历史
const handleDelete = (row: OperationHistoryVO) => {
  ElMessageBox.confirm(`确认删除ID为 ${row.id} 的操作历史记录吗？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteHistory(row.id)
      ElMessage.success('删除成功')
      getHistoryList()
    } catch (error) {
      console.error('删除操作历史失败', error)
      ElMessage.error('删除操作历史失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  getHistoryList()
  getModels()
})
</script>

<style scoped>
.operation-history-container {
  padding: 20px;
}

.filter-container {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.table-images {
  display: flex;
  gap: 12px;
  padding: 8px 0;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80px;
}

.thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

.image-label {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
  text-align: center;
}

.image-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #f5f7fa;
  color: #909399;
}

/* 详情弹窗样式 */
.detail-content {
  padding: 0 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.detail-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 16px;
}

.detail-image-item {
  display: flex;
  flex-direction: column;
}

.detail-image-item h4 {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  text-align: center;
}

.detail-image-item .el-image {
  width: 100%;
  height: 200px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
</style>

<style>
/* 修复图片预览遮罩层问题 */
:root {
  --el-image-viewer-z-index: 3000;
}

.el-image-viewer__wrapper {
  position: fixed !important;
}

.el-image-viewer__mask {
  position: fixed !important;
  background-color: rgba(0, 0, 0, 0.9) !important;
}
</style> 