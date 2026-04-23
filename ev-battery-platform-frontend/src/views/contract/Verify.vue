<template>
  <div class="page-shell">
    <section class="panel-card verify-head">
      <div>
        <p>合同查验</p>
        <h2>合同查验</h2>
        <span>PDF 校验</span>
      </div>
    </section>

    <section class="panel-card verify-panel">
      <el-form :model="form" label-position="top">
        <el-row :gutter="18">
          <el-col :md="12" :xs="24">
            <el-form-item label="合同编号">
              <el-input v-model="form.contractNo" placeholder="请输入或从下方快捷选择" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="上传合同 PDF">
              <el-upload :auto-upload="false" :show-file-list="true" :limit="1" accept=".pdf,application/pdf" :on-change="onChange">
                <el-button>上传 PDF 文件</el-button>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="contractOptions.length" label="快捷选择我的合同">
          <div class="quick-list">
            <el-button v-for="item in contractOptions" :key="item.id" @click="pickContract(item.contractNo)">{{ item.contractNo }}</el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="uploadHistory.length" label="已上传文件记录">
          <el-radio-group v-model="selectedUploadId" class="history-list" @change="pickUploadHistory">
            <el-radio
              v-for="item in uploadHistory"
              :key="item.id"
              :label="item.id"
              border
              class="history-item"
            >
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.contractNo || '未关联合同编号' }} · {{ item.createdAt }}</p>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-button type="primary" :loading="submitting" @click="submit">开始查验</el-button>
      </el-form>

      <div v-if="result" class="result-box" :class="{ success: result.valid, error: !result.valid }">
        <strong>{{ result.valid ? '查验通过' : '查验异常' }}</strong>
        <p>合同号：{{ result.contractNo }}</p>
        <p>结论：{{ result.message }}</p>
        <p>原始哈希：{{ result.storedHash }}</p>
        <p>当前哈希：{{ result.currentHash }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listContracts, verifyContract } from '../../api/contract'

const HISTORY_KEY = 'contract_verify_upload_history'
const fileCache = new Map()

const form = reactive({ contractNo: '', file: null })
const submitting = ref(false)
const result = ref(null)
const contractOptions = ref([])
const uploadHistory = ref([])
const selectedUploadId = ref('')

const normalizeContract = (item = {}) => ({
  ...item,
  contractNo: item.contractNo || item.contract_no || ''
})

const formatNow = () => new Date().toLocaleString('zh-CN', { hour12: false })

const loadUploadHistory = () => {
  try {
    uploadHistory.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
  } catch (error) {
    uploadHistory.value = []
  }
}

const persistUploadHistory = () => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(uploadHistory.value.slice(0, 8)))
}

const pushUploadHistory = (file) => {
  const record = {
    id: `${Date.now()}-${file.name}`,
    name: file.name,
    contractNo: form.contractNo || '',
    createdAt: formatNow()
  }
  fileCache.set(record.id, file)
  uploadHistory.value = [record, ...uploadHistory.value.filter((item) => item.name !== file.name)].slice(0, 8)
  selectedUploadId.value = record.id
  persistUploadHistory()
}

const pickContract = (contractNo) => {
  form.contractNo = contractNo
}

const pickUploadHistory = (value) => {
  const file = fileCache.get(value)
  if (!file) {
    ElMessage.warning('该记录来自历史会话，请重新上传 PDF 文件后再查验')
    return
  }
  form.file = file
  const record = uploadHistory.value.find((item) => item.id === value)
  if (record?.contractNo && !form.contractNo) {
    form.contractNo = record.contractNo
  }
  ElMessage.success(`已选中 ${record?.name || 'PDF 文件'}`)
}

const onChange = (uploadFile) => {
  form.file = uploadFile.raw
  pushUploadHistory(uploadFile.raw)
  ElMessage.success(`已选中 ${uploadFile.name}`)
}

const loadContracts = async () => {
  const res = await listContracts({ page: 1, size: 6 })
  contractOptions.value = (res?.data?.records || []).map(normalizeContract)
}

const submit = async () => {
  if (!form.contractNo) {
    ElMessage.warning('请输入合同编号')
    return
  }
  if (!form.file) {
    ElMessage.warning('请上传需要查验的 PDF 文件')
    return
  }
  const activeHistory = uploadHistory.value.find((item) => item.id === selectedUploadId.value)
  if (activeHistory) {
    activeHistory.contractNo = form.contractNo
    persistUploadHistory()
  }
  submitting.value = true
  try {
    const res = await verifyContract(form)
    result.value = res?.data || res || null
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadContracts()
  loadUploadHistory()
})
</script>

<style scoped>
.verify-head,
.verify-panel {
  padding: 24px;
}

.verify-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.verify-head h2 {
  margin: 0 0 8px;
}

.verify-head span,
.history-item p {
  color: var(--app-muted);
}

.quick-list,
.history-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.history-item {
  margin-right: 0;
}

.result-box {
  margin-top: 22px;
  padding: 20px;
  border-radius: 18px;
}

.result-box.success {
  background: rgba(34, 197, 94, 0.1);
  color: #166534;
}

.result-box.error {
  background: rgba(239, 68, 68, 0.08);
  color: #991b1b;
}

.result-box p {
  margin: 10px 0 0;
  word-break: break-all;
}
</style>


