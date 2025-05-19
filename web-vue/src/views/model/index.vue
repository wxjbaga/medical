<template>
  <div class="container">
    <div class="header">
      <h2>模型管理</h2>
      <div class="actions">
        <el-button type="primary" @click="openCreateModelDialog">
          <el-icon><Plus /></el-icon>新建模型
        </el-button>
        <el-button @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-input
          v-model="searchParams.name"
          placeholder="搜索模型名称"
          class="search-input"
          clearable
          @clear="handleSearch"
        >
          <template #suffix>
            <el-icon class="el-input__icon" @click="handleSearch">
              <Search />
            </el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="modelList"
      border
      style="width: 100%"
      class="table"
    >
      <el-table-column label="模型名称" prop="name" min-width="180" />
      <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
      <el-table-column label="数据集" min-width="180">
        <template #default="{ row }">
          <span>{{ row.datasetName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <model-status :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column fixed="right" prop="operate" label="操作" width="250">
        <template #default="scope">
          <div class="operation">
            <el-button 
              type="primary" 
              size="small" 
              link
              @click="viewModelDetail(scope.row.id)"
            >
              详情
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              link 
              @click="openTrainModelDialog(scope.row.id, scope.row.datasetId)" 
              v-if="scope.row.status !== STATUS_PUBLISHED && scope.row.status !== STATUS_TRAINING"
            >
              训练
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              link 
              @click="handlePublishModel(scope.row.id)" 
              v-if="scope.row.status === STATUS_TRAINED_SUCCESS"
            >
              发布模型
            </el-button>
            <el-button
              type="warning"
              size="small"
              link
              @click="handleUnpublishModel(scope.row.id)"
              v-if="scope.row.status === STATUS_PUBLISHED"
            >
              取消发布
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              link 
              @click="openEditModelDialog(scope.row)" 
              v-if="scope.row.status !== STATUS_PUBLISHED && scope.row.status !== STATUS_TRAINING"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              link 
              @click="handleDeleteModel(scope.row.id)"
              v-if="scope.row.status !== STATUS_PUBLISHED"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="searchParams.current"
        v-model:page-size="searchParams.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 创建模型对话框 -->
    <el-dialog
      v-model="createModelDialog.visible"
      title="新建模型"
      width="500px"
    >
      <el-form
        ref="createModelFormRef"
        :model="createModelDialog.form"
        :rules="createModelDialog.rules"
        label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="createModelDialog.form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createModelDialog.form.description"
            type="textarea"
            placeholder="请输入模型描述"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="数据集" prop="datasetId">
          <el-select
            v-model="createModelDialog.form.datasetId"
            placeholder="请选择数据集"
            style="width: 100%"
          >
            <el-option
              v-for="item in datasetOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createModelDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateModel" :loading="createModelDialog.loading">
            创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 训练模型对话框 -->
    <el-dialog
      v-model="trainModelDialog.visible"
      title="训练模型"
      width="600px"
    >
      <el-form
        ref="trainModelFormRef"
        :model="trainModelDialog.form"
        label-width="120px"
      >
        <el-form-item label="模型名称">
          <el-input v-model="trainModelDialog.modelName" disabled />
        </el-form-item>
        
        <el-divider content-position="left">数据集设置</el-divider>
        
        <el-form-item label="当前数据集">
          <div class="dataset-info">
            <span>{{ trainModelDialog.datasetName }}</span>
            <el-button 
              v-if="trainModelDialog.datasetId" 
              type="primary" 
              size="small" 
              @click="handleDownloadDataset(trainModelDialog.modelId)"
              :loading="trainModelDialog.downloadLoading"
            >
              下载数据集
            </el-button>
          </div>
        </el-form-item>
        
        <el-divider content-position="left">训练超参数</el-divider>
        
        <el-form-item label="输入图像大小" prop="inputSize">
          <el-input-number 
            v-model="trainModelDialog.form.inputSize" 
            :min="32" 
            :max="1024" 
            :step="32" 
          />
        </el-form-item>
        
        <el-form-item label="输入通道数" prop="inChannels">
          <el-input-number 
            v-model="trainModelDialog.form.inChannels" 
            :min="1" 
            :max="4" 
            :step="1" 
          />
        </el-form-item>
        
        <el-form-item label="类别数量" prop="numClasses">
          <el-input-number 
            v-model="trainModelDialog.form.numClasses" 
            :min="1" 
            :max="20" 
            :step="1" 
          />
        </el-form-item>
        
        <el-form-item label="训练轮数" prop="epochs">
          <el-input-number 
            v-model="trainModelDialog.form.epochs" 
            :min="1" 
            :max="500" 
            :step="10" 
          />
        </el-form-item>
        
        <el-form-item label="批量大小" prop="batchSize">
          <el-input-number 
            v-model="trainModelDialog.form.batchSize" 
            :min="1" 
            :max="64" 
            :step="1" 
          />
        </el-form-item>
        
        <el-form-item label="学习率" prop="learningRate">
          <el-input-number 
            v-model="trainModelDialog.form.learningRate" 
            :min="0.000001" 
            :max="0.1" 
            :step="0.0001"
            :precision="6"
            :controls="false"
          />
        </el-form-item>
        
        <el-form-item label="数据增强级别" prop="augLevel">
          <el-select v-model="trainModelDialog.form.augLevel">
            <el-option label="无增强" value="none" />
            <el-option label="轻度增强" value="light" />
            <el-option label="中度增强" value="medium" />
            <el-option label="强度增强" value="strong" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <div v-if="trainModelDialog.hasBeenTrained" class="warning-message">
        <el-alert
          title="警告：重新训练将会删除之前的训练结果和模型权重文件！"
          type="warning"
          :closable="false"
        />
      </div>
      
      <div v-if="trainModelDialog.errorMsg" class="error-message">
        <el-alert
          :title="`上次训练失败原因: ${trainModelDialog.errorMsg}`"
          type="error"
          :closable="false"
        />
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="trainModelDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleTrainModel" :loading="trainModelDialog.loading">
            开始训练
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑模型对话框 -->
    <el-dialog
      v-model="editModelDialog.visible"
      title="编辑模型"
      width="500px"
    >
      <el-form
        ref="editModelFormRef"
        :model="editModelDialog.form"
        :rules="editModelDialog.rules"
        label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="editModelDialog.form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="editModelDialog.form.description"
            type="textarea"
            placeholder="请输入模型描述"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="当前数据集">
          <div class="dataset-info">
            <span>{{ editModelDialog.currentDatasetName }}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="更换数据集">
          <el-select 
            v-model="editModelDialog.form.datasetId" 
            placeholder="选择数据集"
            style="width: 100%"
            :disabled="editModelDialog.datasetLoading"
            @change="handleEditDatasetChange"
          >
            <el-option
              v-for="item in datasetOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="select-tip">
            仅显示已验证成功的数据集
          </div>
          <div v-if="editModelDialog.datasetChanged" class="info-message">
            数据集已更换为: {{ getDatasetNameById(editModelDialog.form.datasetId) }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editModelDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleEditModel" :loading="editModelDialog.loading">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加详情页打开训练对话框支持 -->
    <router-view @openTrainDialog="openTrainModelDialogFromDetail"></router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { 
  getModelList, 
  createModel, 
  trainModel, 
  deleteModel, 
  publishModel,
  updateModelStatus,
  updateModelTrainParams,
  getModelDetail,
  updateModelInfo,
  unpublishModel
} from '@/api/model'
import { getDatasetPage } from '@/api/dataset'
import { formatDateTime } from '@/utils/format'
import { fileRequest } from '@/api/file_request'
import ModelStatus from '@/components/ModelStatus.vue'

const router = useRouter()

// 状态相关
const STATUS_UNTRAINED = 0
const STATUS_TRAINING = 1
const STATUS_TRAINED_SUCCESS = 2
const STATUS_TRAINED_FAILED = 3
const STATUS_PUBLISHED = 4

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case STATUS_UNTRAINED: return '未训练'
    case STATUS_TRAINING: return '训练中'
    case STATUS_TRAINED_SUCCESS: return '训练成功'
    case STATUS_TRAINED_FAILED: return '训练失败'
    case STATUS_PUBLISHED: return '已发布'
    default: return '未知状态'
  }
}

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case STATUS_UNTRAINED: return 'info'
    case STATUS_TRAINING: return 'warning'
    case STATUS_TRAINED_SUCCESS: return 'success'
    case STATUS_TRAINED_FAILED: return 'danger'
    case STATUS_PUBLISHED: return 'success'
    default: return 'info'
  }
}

// 模型列表数据
const loading = ref(false)
const modelList = ref<any[]>([])
const total = ref(0)
const searchParams = reactive({
  current: 1,
  size: 10,
  name: '',
})

// 自动刷新定时器
const autoRefreshTimer = ref<number | null>(null)
const autoRefreshInterval = 10000 // 10秒刷新一次

// 设置自动刷新
const startAutoRefresh = () => {
  // 先清除可能存在的定时器
  stopAutoRefresh()
  
  // 设置新的定时器
  autoRefreshTimer.value = window.setInterval(() => {
    // 检查是否有正在训练中的模型
    const hasTrainingModel = modelList.value.some(model => model.status === STATUS_TRAINING)
    
    // 只有在有训练中模型时才自动刷新
    if (hasTrainingModel) {
      fetchModelList(false) // 静默刷新，不显示加载状态
    } else {
      // 如果没有训练中的模型，停止自动刷新
      stopAutoRefresh()
    }
  }, autoRefreshInterval)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    window.clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

// 在组件卸载前清除定时器
onBeforeUnmount(() => {
  stopAutoRefresh()
})

// 数据集选项
const datasetOptions = ref<{ label: string, value: number }[]>([])

// 创建模型对话框
const createModelFormRef = ref()
const createModelDialog = reactive({
  visible: false,
  loading: false,
  form: {
    name: '',
    description: '',
    datasetId: undefined as number | undefined,
  },
  rules: {
    name: [
      { required: true, message: '请输入模型名称', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    datasetId: [
      { required: true, message: '请选择数据集', trigger: 'change' }
    ]
  }
})

// 训练模型对话框
const trainModelFormRef = ref()
const trainModelDialog = reactive({
  visible: false,
  loading: false,
  downloadLoading: false,
  datasetLoading: false,
  modelId: 0,
  modelName: '',
  datasetId: 0,
  datasetName: '',
  hasBeenTrained: false,
  errorMsg: null,
  form: {
    inputSize: 256,
    inChannels: 1,
    numClasses: 2,
    epochs: 50,
    batchSize: 8,
    learningRate: 0.0001,
    augLevel: 'medium'
  }
})

// 编辑模型对话框
const editModelDialog = reactive({
  visible: false,
  loading: false,
  form: {
    id: 0,
    name: '',
    description: '',
    datasetId: undefined as number | undefined,
  },
  rules: {
    name: [
      { required: true, message: '请输入模型名称', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    datasetId: [
      { required: true, message: '请选择数据集', trigger: 'change' }
    ]
  },
  currentDatasetName: '',
  datasetLoading: false,
  datasetChanged: false,
  hasTrainedData: false,
  originalDatasetId: undefined as number | undefined
})

// 获取模型列表
const fetchModelList = async (showLoading = true) => {
  try {
    if (showLoading) {
      loading.value = true
    }
    
    const res = await getModelList(searchParams)
    modelList.value = res.records
    total.value = res.total
    
    // 检查是否有训练中的模型，如果有，开启自动刷新
    const hasTrainingModel = res.records.some((model: any) => model.status === STATUS_TRAINING)
    if (hasTrainingModel) {
      startAutoRefresh()
    }
  } catch (error: any) {
    ElMessage.error('获取模型列表失败: ' + error.message)
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 获取数据集列表（用于创建模型时选择）
const fetchDatasetOptions = async () => {
  try {
    const res = await getDatasetPage({
      current: 1,
      size: 1000,
      status: 2 // 已验证成功的数据集
    })
    if (res && res.records) {
      datasetOptions.value = res.records.map((item: any) => ({
        label: item.name,
        value: item.id
      }))
    }
  } catch (error: any) {
    ElMessage.error('获取数据集列表失败: ' + error.message)
  }
}

// 搜索
const handleSearch = () => {
  searchParams.current = 1
  fetchModelList()
}

// 分页处理
const handleSizeChange = (size: number) => {
  searchParams.size = size
  fetchModelList()
}

const handleCurrentChange = (current: number) => {
  searchParams.current = current
  fetchModelList()
}

// 打开创建模型对话框
const openCreateModelDialog = () => {
  createModelDialog.form = {
    name: '',
    description: '',
    datasetId: undefined
  }
  createModelDialog.visible = true
  // 获取可用的数据集列表
  fetchDatasetOptions()
}

// 创建模型
const handleCreateModel = async () => {
  if (!createModelFormRef.value) return
  
  await createModelFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      createModelDialog.loading = true
      const formData = { 
        name: createModelDialog.form.name,
        description: createModelDialog.form.description,
        datasetId: createModelDialog.form.datasetId as number // 类型断言为 number
      }
      // 确保 datasetId 有值
      if (formData.datasetId === undefined) {
        ElMessage.warning('请选择数据集')
        return
      }
      await createModel(formData)
      ElMessage.success('模型创建成功')
      createModelDialog.visible = false
      fetchModelList()
    } catch (error: any) {
      ElMessage.error('创建模型失败: ' + error.message)
    } finally {
      createModelDialog.loading = false
    }
  })
}

// 根据ID获取数据集名称
const getDatasetNameById = (id: number | undefined) => {
  if (!id) return '';
  const dataset = datasetOptions.value.find(item => item.value === id);
  return dataset ? dataset.label : '';
}

// 打开训练模型对话框
const openTrainModelDialog = async (modelId: number, datasetId: number) => {
  try {
    // 先获取最新的模型信息，确保状态是最新的
    const modelDetail = await getModelDetail(modelId);
    
    // 检查模型是否正在训练中
    if (modelDetail.status === STATUS_TRAINING) {
      ElMessage.warning('模型正在训练中，请等待训练完成后再操作');
      return;
    }
    
    trainModelDialog.modelId = modelDetail.id
    trainModelDialog.modelName = modelDetail.name
    trainModelDialog.datasetId = modelDetail.datasetId
    trainModelDialog.datasetName = modelDetail.datasetName
    trainModelDialog.hasBeenTrained = modelDetail.status === STATUS_TRAINED_SUCCESS || modelDetail.status === STATUS_TRAINED_FAILED || modelDetail.status === STATUS_PUBLISHED
    trainModelDialog.errorMsg = modelDetail.status === STATUS_TRAINED_FAILED ? modelDetail.errorMsg : null
    
    // 如果有训练超参数，使用已有的
    if (modelDetail.trainHyperparams) {
      try {
        const hyperparams = JSON.parse(modelDetail.trainHyperparams)
        trainModelDialog.form = {
          ...trainModelDialog.form,
          ...hyperparams
        }
      } catch (e) {
        // 解析失败使用默认值
        console.error('解析训练超参数失败', e)
      }
    }
    
    trainModelDialog.visible = true
  } catch (error: any) {
    ElMessage.error('获取模型详情失败: ' + error.message);
  }
}

// 下载数据集
const handleDownloadDataset = async (modelId: number) => {
  try {
    trainModelDialog.downloadLoading = true
    
    // 先获取模型详情，获取模型关联的数据集信息
    const modelDetail = await getModelDetail(modelId)
    
    if (!modelDetail.datasetBucket || !modelDetail.datasetObjectKey) {
      ElMessage.error('未找到模型关联的数据集信息')
      return
    }
    
    // 使用file_request直接下载文件
    const response = await fileRequest.get(modelDetail.datasetBucket, modelDetail.datasetObjectKey)
    
    // 创建下载链接
    const blob = new Blob([response], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `dataset_for_model_${modelId}.zip`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('数据集下载已开始')
  } catch (error: any) {
    ElMessage.error('下载数据集失败: ' + error.message)
  } finally {
    trainModelDialog.downloadLoading = false
  }
}

// 开始训练模型
const handleTrainModel = async () => {
  if (trainModelDialog.hasBeenTrained) {
    try {
      await ElMessageBox.confirm(
        '重新训练将会删除之前的训练结果和模型权重文件，确定要继续吗？',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (e) {
      return
    }
  }
  
  try {
    trainModelDialog.loading = true
    
    // 再次检查模型当前状态，确保没有并发操作
    const modelDetail = await getModelDetail(trainModelDialog.modelId);
    if (modelDetail.status === STATUS_TRAINING) {
      ElMessage.warning('模型正在训练中，请等待训练完成后再操作');
      trainModelDialog.loading = false;
      return;
    }
    
    // 直接传递表单对象，不需要转成字符串
    await trainModel(trainModelDialog.modelId, trainModelDialog.form);
    
    ElMessage.success('模型训练已开始，请稍后查看结果')
    trainModelDialog.visible = false
    
    // 获取最新列表并启动自动刷新
    await fetchModelList()
    startAutoRefresh()
  } catch (error: any) {
    if (error.response && error.response.data && error.response.data.msg) {
      ElMessage.error('启动训练失败: ' + error.response.data.msg);
    } else {
      ElMessage.error('启动训练失败: ' + error.message);
    }
  } finally {
    trainModelDialog.loading = false
  }
}

// 删除模型
const handleDeleteModel = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该模型吗？删除后将无法恢复！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteModel(id)
    ElMessage.success('模型删除成功')
    fetchModelList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除模型失败: ' + error.message)
    }
  }
}

// 发布模型
const handlePublishModel = async (id: number) => {
  try {
    await publishModel(id)
    ElMessage.success('模型发布成功')
    fetchModelList()
  } catch (error: any) {
    ElMessage.error('发布模型失败: ' + error.message)
  }
}

// 取消发布模型
const handleUnpublishModel = async (modelId: number) => {
  try {
    await ElMessageBox.confirm('确定要取消发布该模型吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const result = await unpublishModel(modelId)
    if (result) {
      ElMessage.success('取消发布成功')
      fetchModelList()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('取消发布失败: ' + error.message)
    }
  }
}

// 查看模型详情
const viewModelDetail = (id: number) => {
  router.push(`/model/detail/${id}`)
}

// 打开编辑模型对话框
const openEditModelDialog = async (row: any) => {
  try {
    // 获取模型详情，检查是否有训练数据
    const modelDetail = await getModelDetail(row.id)
    editModelDialog.hasTrainedData = modelDetail.status === STATUS_TRAINED_SUCCESS || 
                                    modelDetail.status === STATUS_TRAINED_FAILED || 
                                    modelDetail.status === STATUS_PUBLISHED
    
    editModelDialog.form.id = row.id
    editModelDialog.form.name = row.name
    editModelDialog.form.description = row.description
    editModelDialog.form.datasetId = row.datasetId
    editModelDialog.originalDatasetId = row.datasetId
    editModelDialog.currentDatasetName = row.datasetName
    editModelDialog.datasetChanged = false
    editModelDialog.visible = true
    fetchDatasetOptions()
  } catch (error: any) {
    ElMessage.error('获取模型详情失败: ' + error.message)
  }
}

// 编辑模型
const handleEditModel = async () => {
  if (!editModelDialog.form.name || !editModelDialog.form.description || !editModelDialog.form.datasetId) {
    ElMessage.warning('请填写完整的模型信息')
    return
  }

  // 只有在模型有训练数据且数据集发生变化时才提示
  if (editModelDialog.hasTrainedData && editModelDialog.datasetChanged) {
    try {
      await ElMessageBox.confirm(
        '更换数据集将会清空模型之前的训练数据和权重文件，确定要继续吗？',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消', 
          type: 'warning'
        }
      )
    } catch (e) {
      return // 用户取消操作
    }
  }

  try {
    editModelDialog.loading = true
    await updateModelInfo({
      id: editModelDialog.form.id,
      name: editModelDialog.form.name,
      description: editModelDialog.form.description,
      datasetId: editModelDialog.form.datasetId
    })
    ElMessage.success('模型编辑成功')
    editModelDialog.visible = false
    fetchModelList()
  } catch (error: any) {
    ElMessage.error('编辑模型失败: ' + error.message)
  } finally {
    editModelDialog.loading = false
  }
}

// 处理数据集变更
const handleEditDatasetChange = (value: number) => {
  if (value) {
    if (value !== editModelDialog.originalDatasetId) {
      editModelDialog.datasetChanged = true;
    } else {
      editModelDialog.datasetChanged = false;
    }
    editModelDialog.form.datasetId = value;
  } else {
    editModelDialog.datasetChanged = false;
    editModelDialog.form.datasetId = undefined;
  }
}

// 初始化
onMounted(() => {
  fetchModelList()
  
  // 检查URL参数，如果是从详情页面跳转来训练模型的
  const query = router.currentRoute.value.query
  if (query.action === 'train' && query.id) {
    const modelId = Number(query.id)
    if (!isNaN(modelId)) {
      // 获取模型详情并打开训练对话框
      openTrainModelDialogById(modelId)
    }
  }
})

// 通过ID打开训练对话框
const openTrainModelDialogById = async (modelId: number) => {
  try {
    loading.value = true
    // 获取模型详情
    const modelDetail = await getModelDetail(modelId)
    if (modelDetail) {
      openTrainModelDialog(modelDetail.id, modelDetail.datasetId)
    } else {
      ElMessage.warning('未找到指定的模型')
    }
  } catch (error: any) {
    ElMessage.error('获取模型详情失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 从详情页打开训练对话框
const openTrainModelDialogFromDetail = async (modelId: number) => {
  try {
    const model = await getModelDetail(modelId)
    openTrainModelDialog(model.id, model.datasetId)
  } catch (error: any) {
    ElMessage.error('获取模型详情失败: ' + error.message)
  }
}

// 刷新列表
const handleRefresh = () => {
  fetchModelList()
}
</script>

<style scoped>
.container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 250px;
}

.table {
  margin-bottom: 20px;
}

.dataset-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.warning-message {
  margin-top: 20px;
  margin-bottom: 10px;
}

.info-message {
  margin-top: 8px;
  color: #409EFF;
  font-size: 13px;
}

.select-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.error-message {
  margin-top: 8px;
  color: #F56C6C;
  font-size: 13px;
}
</style> 