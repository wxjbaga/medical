<template>
  <div class="dataset-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>数据集管理</span>
          <el-button type="primary" @click="handleAddDataset">添加数据集</el-button>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="search-form">
        <el-form-item label="数据集名称" prop="name">
          <el-input v-model="queryParams.name" placeholder="请输入数据集名称" clearable />
        </el-form-item>
        <el-form-item label="验证状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择验证状态" clearable style="width: 150px">
            <el-option label="未验证" :value="0" />
            <el-option label="验证中" :value="1" />
            <el-option label="验证成功" :value="2" />
            <el-option label="验证失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 数据集表格 -->
      <el-table :data="datasetList" border style="width: 100%" :row-class-name="getRowClassName">
        <!-- <el-table-column prop="id" label="ID" width="80" /> -->
        <el-table-column prop="name" label="数据集名称" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="trainCount" label="训练样例数" width="100" />
        <el-table-column prop="valCount" label="验证样例数" width="100" />
        <el-table-column label="验证状态" width="120">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.status === 3"
              :content="'验证失败: ' + (row.errorMsg ? getShortErrorMsg(row.errorMsg) : '未知错误')"
              placement="top"
            >
              <el-tag :type="getStatusTagType(row.status)">
                {{ row.statusName }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="getStatusTagType(row.status)">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createUser.realName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <div class="operation-btns">
              <el-button type="primary" size="small" @click="handleUpload(row)">上传</el-button>
              <el-button v-if="row.status === 0 || row.status === 3" type="success" size="small" @click="handleValidate(row)">验证</el-button>
              <el-button v-if="row.bucket && row.objectKey" type="success" size="small" @click="handleDownload(row)">下载</el-button>
              <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              <el-button v-if="row.status === 3" type="info" size="small" @click="showErrorMsg(row)">查看错误</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页组件 -->
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
    </el-card>
    
    <!-- 添加数据集对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加数据集" width="500px" :before-close="handleAddDialogClose">
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="120px">
        <el-form-item label="数据集名称" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="数据集描述" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请输入数据集描述（选填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleAddDialogClose">取消</el-button>
          <el-button type="primary" @click="submitAddForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 编辑数据集对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑数据集" width="500px" :before-close="handleEditDialogClose">
      <el-form ref="editFormRef" :model="editForm" :rules="addFormRules" label-width="120px">
        <el-form-item label="数据集名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="数据集描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入数据集描述（选填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleEditDialogClose">取消</el-button>
          <el-button type="primary" @click="submitEditForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 上传数据集对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传数据集文件" width="500px" :before-close="handleUploadDialogClose">
      <div class="upload-container">
        <el-alert
          title="请上传ZIP格式的数据集文件"
          type="warning"
          description="2D数据集需包含train/images、train/labels、val/images、val/labels目录"
          :closable="false"
          show-icon
        />
        <el-upload
          class="upload-demo"
          drag
          :action="uploadAction"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="() => { ElMessage.warning('最多只能上传一个文件') }"
          :before-remove="() => { console.log('准备移除文件'); return true }"
          :limit="1"
          :file-list="uploadFileList"
          accept=".zip"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">请上传ZIP格式的数据集文件</div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleUploadDialogClose">取消</el-button>
          <el-button type="primary" @click="submitUploadForm" :disabled="!uploadFile">上传</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 错误消息对话框 -->
    <el-dialog v-model="errorDialogVisible" title="验证错误详情" width="600px" align-center>
      <div class="error-details">
        <div class="error-header">
          <el-alert
            title="数据集验证失败"
            type="error"
            description="以下是验证过程中发现的错误信息，请根据错误提示修复问题后重新上传验证。"
            :closable="false"
            show-icon
          />
        </div>
        <div class="error-toolbar">
          <el-button type="primary" size="small" @click="copyErrorMsg" icon="CopyDocument">
            复制错误信息
          </el-button>
        </div>
        <div class="error-content">
          <pre class="error-msg">{{ currentErrorMsg }}</pre>
        </div>
        <div class="error-actions">
          <el-button type="primary" @click="handleErrorUpload" icon="Upload">重新上传</el-button>
          <el-button @click="errorDialogVisible = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { UploadFilled, CopyDocument, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import type { DatasetVO, DatasetAddDTO, DatasetQueryDTO } from '@/types/dataset'
import type { PageVO } from '@/types/common'
import { addDataset, deleteDataset, getDatasetPage, uploadDataset, validateDataset, downloadDataset, updateDataset } from '@/api/dataset'
import { useUserStore } from '@/stores/user'
import { StatusConstant } from '@/constants/status'

// 用户信息
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

// 查询参数
const queryParams = reactive<DatasetQueryDTO>({
  current: 1,
  size: 10,
  name: '',
  status: undefined
})

// 表格数据
const datasetList = ref<DatasetVO[]>([])
const total = ref(0)

// 表单引用
const queryForm = ref<FormInstance>()
const addFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

// 添加数据集表单
const addDialogVisible = ref(false)
const addForm = reactive<DatasetAddDTO>({
  name: '',
  description: ''
})

// 添加表单校验规则
const addFormRules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
  ]
})

// 编辑数据集表单
const editDialogVisible = ref(false)
const editForm = reactive<DatasetAddDTO & { id?: number }>({
  name: '',
  description: ''
})

// 上传数据集
const uploadDialogVisible = ref(false)
const currentDatasetId = ref<number | null>(null)
const uploadFile = ref<UploadFile | null>(null)
const uploadFileList = ref<UploadFile[]>([])
const uploadAction = computed(() => currentDatasetId.value ? `/dataset/upload/${currentDatasetId.value}` : '')

// 错误消息
const errorDialogVisible = ref(false)
const currentErrorMsg = ref('')
const currentErrorDataset = ref<DatasetVO | null>(null)

// 定义轮询验证状态的间隔时间（毫秒）
const POLL_INTERVAL = 5000 // 5秒
let pollingTimer: number | null = null

// 生命周期钩子
onMounted(() => {
  getDatasetList()
  // 启动轮询
  startPolling()
})

// 组件卸载前清除定时器
onBeforeUnmount(() => {
  stopPolling()
})

// 开始轮询
const startPolling = () => {
  if (pollingTimer !== null) {
    stopPolling()
  }
  
  pollingTimer = window.setInterval(() => {
    // 检查是否有正在验证中的数据集
    if (datasetList.value.some(dataset => dataset.status === 1)) {
      getDatasetList()
    }
  }, POLL_INTERVAL)
}

// 停止轮询
const stopPolling = () => {
  if (pollingTimer !== null) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 获取数据集列表
const getDatasetList = async () => {
  try {
    const pageData = await getDatasetPage(queryParams) as PageVO<DatasetVO>
    datasetList.value = pageData.records
    total.value = pageData.total
    
    // 检查验证失败的数据集并通知
    checkAndNotifyFailedDatasets()
  } catch (error) {
    console.error('获取数据集列表失败', error)
  }
}

// 查询
const handleQuery = () => {
  queryParams.current = 1
  getDatasetList()
}

// 重置查询
const resetQuery = () => {
  queryForm.value?.resetFields()
  queryParams.current = 1
  getDatasetList()
}

// 处理页码大小变化
const handleSizeChange = (val: number) => {
  queryParams.size = val
  getDatasetList()
}

// 处理页码变化
const handleCurrentChange = (val: number) => {
  queryParams.current = val
  getDatasetList()
}

// 添加数据集
const handleAddDataset = () => {
  addForm.name = ''
  addForm.description = ''
  addDialogVisible.value = true
}

// 关闭添加对话框
const handleAddDialogClose = () => {
  addDialogVisible.value = false
}

// 提交添加表单
const submitAddForm = async () => {
  const formInstance = addFormRef.value
  if (!formInstance) return
  
  await formInstance.validate(async (valid) => {
    if (valid) {
      try {
        const datasetId = await addDataset(addForm)
        ElMessage.success('添加数据集成功')
        addDialogVisible.value = false
        getDatasetList()
        
        // 提示用户上传数据集
        ElMessageBox.confirm(
          '数据集创建成功，是否立即上传数据集文件？',
          '提示',
          {
            confirmButtonText: '上传',
            cancelButtonText: '稍后上传',
            type: 'info'
          }
        ).then(() => {
          // 打开上传对话框
          currentDatasetId.value = datasetId
          uploadDialogVisible.value = true
        }).catch(() => {
          // 稍后上传，不做任何操作
        })
      } catch (error) {
        console.error('添加数据集失败', error)
        ElMessage.error('添加数据集失败')
      }
    }
  })
}

// 上传数据集
const handleUpload = (row: DatasetVO) => {
  currentDatasetId.value = row.id
  uploadFile.value = null
  uploadFileList.value = []
  uploadDialogVisible.value = true
}

// 关闭上传对话框
const handleUploadDialogClose = () => {
  uploadDialogVisible.value = false
  uploadFile.value = null
  uploadFileList.value = []
}

// 文件移除
const handleFileRemove = (file: UploadFile, fileList: UploadFile[]) => {
  console.log('文件已移除:', file.name, '状态:', file.status)
  console.log('剩余文件列表长度:', fileList.length)
  uploadFile.value = null
  uploadFileList.value = []
}

// 文件变更
const handleFileChange = (file: UploadFile, fileList: UploadFile[]) => {
  console.log('文件变更事件触发，文件状态:', file?.status, '文件列表长度:', fileList.length)
  
  // 当文件状态为有效状态时，表示已选择了文件
  if (file) {
    uploadFile.value = file
    uploadFileList.value = fileList.length > 0 ? [file] : []
    console.log('文件已选择:', file.name, '状态:', file.status)
  } else {
    // 如果文件列表为空，则清空uploadFile
    if (fileList.length === 0) {
      uploadFile.value = null
      uploadFileList.value = []
      console.log('文件列表为空，已清空上传文件')
    } else if (fileList.length > 0 && !uploadFile.value) {
      // 如果文件列表不为空，但uploadFile为空，则使用第一个文件
      uploadFile.value = fileList[0]
      console.log('使用文件列表中的第一个文件:', fileList[0].name)
    }
  }
}

// 提交上传
const submitUploadForm = async () => {
  // 添加日志输出
  console.log('提交上传，当前文件:', uploadFile.value?.name, '状态:', uploadFile.value?.status)
  console.log('文件列表长度:', uploadFileList.value?.length)
  
  // 重新检查文件列表，如果uploadFile为null但文件列表不为空，使用文件列表的第一个文件
  if (!uploadFile.value && uploadFileList.value.length > 0) {
    uploadFile.value = uploadFileList.value[0]
    console.log('从文件列表恢复上传文件:', uploadFile.value.name)
  }
  
  if (!uploadFile.value || !currentDatasetId.value) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  // 检查文件类型
  const fileName = uploadFile.value.name || ''
  if (!fileName.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('请上传ZIP格式的文件')
    return
  }
  
  const formData = new FormData()
  
  // 确保raw属性存在
  if (!uploadFile.value.raw) {
    ElMessage.warning('文件数据无效，请重新选择文件')
    return
  }
  
  try {
    formData.append('file', uploadFile.value.raw as File)
    
    const success = await uploadDataset(currentDatasetId.value, formData)
    if (success) {
      ElMessage.success('上传数据集文件成功，请点击"验证"按钮验证数据集')
      uploadDialogVisible.value = false
      getDatasetList()
    } else {
      ElMessage.error('上传数据集文件失败')
    }
  } catch (error) {
    console.error('上传数据集文件失败', error)
    ElMessage.error('上传数据集文件失败')
  }
}

// 删除数据集
const handleDelete = (row: DatasetVO) => {
  ElMessageBox.confirm(
    `确定要删除数据集 "${row.name}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const success = await deleteDataset(row.id)
      if (success) {
        ElMessage.success('删除数据集成功')
        getDatasetList()
      } else {
        ElMessage.error('删除数据集失败')
      }
    } catch (error) {
      console.error('删除数据集失败', error)
      ElMessage.error('删除数据集失败')
    }
  }).catch(() => {
    // 取消删除，不做任何操作
  })
}

// 编辑数据集
const handleEdit = (row: DatasetVO) => {
  editForm.id = row.id
  editForm.name = row.name
  editForm.description = row.description || ''
  editDialogVisible.value = true
}

// 关闭编辑对话框
const handleEditDialogClose = () => {
  editDialogVisible.value = false
}

// 提交编辑表单
const submitEditForm = async () => {
  const formInstance = editFormRef.value
  if (!formInstance || !editForm.id) return
  
  await formInstance.validate(async (valid) => {
    if (valid) {
      try {
        if (!editForm.id) {
          ElMessage.error('数据集ID不存在')
          return
        }
        const success = await updateDataset(editForm.id, {
          name: editForm.name,
          description: editForm.description
        })
        if (success) {
          ElMessage.success('更新数据集成功')
          editDialogVisible.value = false
          getDatasetList()
        } else {
          ElMessage.error('更新数据集失败')
        }
      } catch (error) {
        console.error('更新数据集失败', error)
        ElMessage.error('更新数据集失败')
      }
    }
  })
}

// 下载数据集
const handleDownload = (row: DatasetVO) => {
  if (!row.bucket || !row.objectKey) {
    ElMessage.warning('数据集文件不存在')
    return
  }
  
  try {
    ElMessage.success('正在准备下载，请稍候...')
    downloadDataset(row.bucket, row.objectKey, row.name)
      .then(() => {
        ElMessage.success('下载已开始')
      })
      .catch((error) => {
        console.error('下载数据集文件失败', error)
        ElMessage.error('下载数据集文件失败')
      })
  } catch (error) {
    console.error('下载数据集文件失败', error)
    ElMessage.error('下载数据集文件失败')
  }
}

// 获取简短的错误信息用于提示
const getShortErrorMsg = (msg: string) => {
  if (!msg) return '未知错误'
  // 截取前50个字符，如果超过则添加省略号
  return msg.length > 50 ? msg.substring(0, 50) + '...' : msg
}

// 显示错误信息
const showErrorMsg = (row: DatasetVO) => {
  currentErrorMsg.value = row.errorMsg || '未知错误'
  currentErrorDataset.value = row
  errorDialogVisible.value = true
}

// 从错误对话框中直接进入上传流程
const handleErrorUpload = () => {
  if (currentErrorDataset.value) {
    errorDialogVisible.value = false
    handleUpload(currentErrorDataset.value)
  }
}

// 获取状态标签类型
const getStatusTagType = (status: number) => {
  switch (status) {
    case 0: return 'info'     // 未验证
    case 1: return 'warning'  // 验证中
    case 2: return 'success'  // 验证成功
    case 3: return 'danger'   // 验证失败
    default: return 'info'
  }
}

// 验证数据集
const handleValidate = (row: DatasetVO) => {
  ElMessageBox.confirm(
    `确定要验证数据集 "${row.name}" 吗？这可能需要几分钟时间。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      const success = await validateDataset(row.id)
      if (success) {
        ElMessage.success('验证请求已提交，系统正在验证数据集')
        getDatasetList()
      } else {
        ElMessage.error('验证数据集失败')
      }
    } catch (error) {
      console.error('验证数据集失败', error)
      ElMessage.error('验证数据集失败')
    }
  }).catch(() => {
    // 取消验证，不做任何操作
  })
}

// 当数据集状态为验证失败时，查看列表后自动提示
const checkAndNotifyFailedDatasets = () => {
  // 查找验证失败的数据集
  const failedDatasets = datasetList.value.filter(dataset => dataset.status === 3)
  if (failedDatasets.length > 0) {
    // 显示通知，告知用户有验证失败的数据集
    ElMessage({
      message: `有 ${failedDatasets.length} 个数据集验证失败，可点击"查看错误"按钮查看详细原因`,
      type: 'warning',
      duration: 5000,
      showClose: true
    })
  }
}

// 监听数据集状态变化
watch(
  () => datasetList.value,
  (newList, oldList) => {
    if (!oldList || oldList.length === 0) return
    
    // 检查数据集状态是否有变化
    newList.forEach(newDataset => {
      const oldDataset = oldList.find(item => item.id === newDataset.id)
      if (oldDataset && oldDataset.status !== newDataset.status) {
        // 状态发生变化，根据新状态显示通知
        if (newDataset.status === StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
          ElNotification({
            title: '验证成功',
            message: `数据集 "${newDataset.name}" 验证成功`,
            type: 'success',
            duration: 5000
          })
        } else if (newDataset.status === StatusConstant.DATASET_STATUS_VERIFIED_FAILED) {
          ElNotification({
            title: '验证失败',
            message: `数据集 "${newDataset.name}" 验证失败，点击查看错误按钮查看详细信息`,
            type: 'error',
            duration: 0, // 不自动关闭
            onClick: () => {
              showErrorMsg(newDataset)
            }
          })
        }
      }
    })
  },
  { deep: true }
)

// 复制错误信息到剪贴板
const copyErrorMsg = () => {
  if (navigator.clipboard && currentErrorMsg.value) {
    navigator.clipboard.writeText(currentErrorMsg.value)
      .then(() => {
        ElMessage.success('错误信息已复制到剪贴板')
      })
      .catch(() => {
        ElMessage.error('复制失败，请手动复制')
      })
  } else {
    ElMessage.error('您的浏览器不支持自动复制，请手动复制')
  }
}

// 获取表格行的类名
const getRowClassName = ({ row }: { row: DatasetVO }) => {
  return row.status === 3 ? 'validation-failed' : ''
}
</script>

<style scoped>
.dataset-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.operation-btns {
  display: flex;
  gap: 5px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-container {
  margin-bottom: 20px;
}

.upload-container .el-alert {
  margin-bottom: 20px;
}

.error-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-header {
  margin-bottom: 10px;
}

.error-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.error-content {
  max-height: 300px;
  overflow-y: auto;
  background-color: #f8f8f8;
  border-radius: 4px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.error-msg {
  white-space: pre-wrap;
  word-break: break-all;
  color: #f56c6c;
  font-family: monospace;
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

/* 验证失败状态的表格行高亮显示 */
:deep(.el-table__row.validation-failed) {
  background-color: rgba(245, 108, 108, 0.1);
}
</style> 