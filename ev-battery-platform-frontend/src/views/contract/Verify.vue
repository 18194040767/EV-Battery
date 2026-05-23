<template>
  <div class="page-shell contract-verify-page">
    <section class="verify-head">
      <div class="verify-head-copy">
        <p>合同存证&nbsp;&nbsp;/&nbsp;&nbsp;合同查验</p>
        <h2>合同查验</h2>
        <span>PDF 校验</span>
      </div>
    </section>

    <section class="verify-card verify-compose-card">
      <el-form :model="form" label-position="top">
        <div class="compose-grid">
          <div class="compose-section contract-section">
            <div class="section-title">
              <span>1</span>
              <strong>合同信息</strong>
            </div>
            <el-form-item label="合同编号">
              <el-input v-model="form.contractNo" placeholder="请输入或从下方快捷选择">
                <template #suffix>
                  <button class="input-menu-button" type="button" aria-label="快捷选择合同">
                    <span></span>
                    <span></span>
                    <span></span>
                  </button>
                </template>
              </el-input>
              <p class="field-note">支持手动输入或从已上传记录中选择</p>
            </el-form-item>
            <div v-if="contractOptions.length" class="quick-list">
              <button
                v-for="item in contractOptions"
                :key="item.id"
                class="quick-contract"
                type="button"
                @click="pickContract(item.contractNo)"
              >
                {{ item.contractNo }}
              </button>
            </div>
          </div>

          <div class="compose-section upload-section">
            <div class="section-title">
              <span>2</span>
              <strong>上传合同 PDF</strong>
            </div>
            <el-upload
              drag
              :auto-upload="false"
              :show-file-list="false"
              :limit="1"
              accept=".pdf,application/pdf"
              :on-change="onChange"
              class="verify-upload"
            >
              <div class="upload-icon">↑</div>
              <p>点击上传或拖拽文件到此处</p>
              <em>支持 PDF 格式，文件大小不超过 50MB</em>
              <el-button type="primary" plain>选择 PDF 文件</el-button>
            </el-upload>
          </div>
        </div>
      </el-form>

      <div v-if="result" class="result-box" :class="{ success: result.valid, error: !result.valid }">
        <strong>{{ result.valid ? '查验通过' : '查验异常' }}</strong>
        <p>合同号：{{ result.contractNo }}</p>
        <p>结论：{{ result.message }}</p>
        <p>原始哈希：{{ result.storedHash }}</p>
        <p>当前哈希：{{ result.currentHash }}</p>
      </div>
    </section>

    <section class="verify-card upload-record-card">
      <div class="section-title record-title">
        <span>3</span>
        <strong>已上传记录</strong>
      </div>

      <div v-if="uploadHistory.length" class="upload-table-wrap">
        <table class="upload-table">
          <thead>
            <tr>
              <th>选择</th>
              <th>文件名称</th>
              <th>合同编号</th>
              <th>上传时间</th>
              <th>文件大小</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in uploadHistory"
              :key="item.id"
              :class="{ active: selectedUploadId === item.id }"
              @click="pickUploadHistory(item.id)"
            >
              <td>
                <button
                  class="select-radio"
                  :class="{ checked: selectedUploadId === item.id }"
                  type="button"
                  :aria-label="`选择 ${item.name}`"
                ></button>
              </td>
              <td>
                <span class="file-cell">
                  <i>PDF</i>
                  {{ item.name }}
                </span>
              </td>
              <td>{{ item.contractNo || '未关联合同编号' }}</td>
              <td>{{ item.createdAt }}</td>
              <td>{{ item.sizeText || '-' }}</td>
              <td>
                <button class="preview-button" type="button" @click.stop="pickUploadHistory(item.id)">预览</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <el-empty v-else description="暂无上传记录" />

      <p class="record-hint">请选择一个文件作为本次查验对象</p>
      <el-button type="primary" class="verify-submit" :loading="submitting" @click="submit">开始查验</el-button>
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

const formatFileSize = (size = 0) => {
  if (!size) return '-'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

const loadUploadHistory = () => {
  try {
    uploadHistory.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    selectedUploadId.value = uploadHistory.value[0]?.id || ''
  } catch (error) {
    uploadHistory.value = []
    selectedUploadId.value = ''
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
    createdAt: formatNow(),
    sizeText: formatFileSize(file.size)
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
  selectedUploadId.value = value
  const file = fileCache.get(value)
  const record = uploadHistory.value.find((item) => item.id === value)
  if (!file) {
    ElMessage.warning('该记录来自历史会话，请重新上传 PDF 文件后再查验')
    return
  }
  form.file = file
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
.contract-verify-page {
  gap: 22px;
  width: 100%;
  margin: 0;
}

.verify-head {
  min-height: 230px;
  overflow: hidden;
  border: 1px solid rgba(211, 224, 247, 0.72);
  border-radius: 18px;
  background:
    linear-gradient(90deg, rgba(246, 250, 255, 0.96) 0%, rgba(246, 250, 255, 0.82) 43%, rgba(246, 250, 255, 0.18) 100%),
    url("/contract-verify-hero.png") center / cover no-repeat;
  box-shadow: 0 18px 42px rgba(38, 96, 178, 0.08);
}

.verify-head-copy {
  padding: 42px;
}

.verify-head p {
  margin: 0 0 22px;
  color: #5f6f8d;
  font-size: 16px;
  line-height: 1;
}

.verify-head h2 {
  margin: 0 0 18px;
  color: #071637;
  font-size: 44px;
  font-weight: 800;
  line-height: 1;
}

.verify-head span {
  color: #495976;
  font-size: 22px;
  line-height: 1;
}

.verify-card {
  border: 1px solid rgba(213, 225, 245, 0.86);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(41, 96, 178, 0.07);
}

.verify-compose-card {
  padding: 28px 28px 26px;
}

.compose-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 28px;
}

.compose-section {
  min-height: 210px;
}

.contract-section {
  padding-right: 30px;
  border-right: 1px solid #dce5f2;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
  color: #162642;
  font-size: 17px;
  line-height: 1;
}

.section-title span {
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(180deg, #4b8dff 0%, #1d6fff 100%);
  box-shadow: 0 6px 14px rgba(31, 117, 255, 0.24);
  font-size: 15px;
  font-weight: 700;
}

.section-title strong {
  font-weight: 700;
}

:deep(.verify-compose-card .el-form-item__label) {
  margin-bottom: 10px;
  color: #4d5d77;
  font-size: 15px;
  line-height: 1;
}

:deep(.verify-compose-card .el-input__wrapper) {
  min-height: 48px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d2dceb inset;
}

:deep(.verify-compose-card .el-input__inner) {
  color: #182946;
  font-size: 15px;
}

:deep(.verify-compose-card .el-input__inner::placeholder) {
  color: #a3b0c3;
}

.input-menu-button {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.input-menu-button span {
  display: block;
  width: 15px;
  height: 2px;
  border-radius: 2px;
  background: #6e7f9a;
}

.field-note {
  width: 100%;
  margin: 14px 0 0;
  color: #77859b;
  font-size: 14px;
  line-height: 1.4;
}

.quick-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: -2px;
}

.quick-contract {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #d8e3f4;
  border-radius: 8px;
  color: #426086;
  background: #f7faff;
  cursor: pointer;
}

:deep(.verify-upload .el-upload) {
  display: block;
}

:deep(.verify-upload .el-upload-dragger) {
  display: flex;
  min-height: 214px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #bcd3f6;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

:deep(.verify-upload .el-upload-dragger:hover) {
  border-color: #2f7cff;
  background: #f8fbff;
}

.upload-icon {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(180deg, #5093ff 0%, #1f75ff 100%);
  box-shadow: 0 8px 18px rgba(31, 117, 255, 0.24);
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.verify-upload p {
  margin: 0 0 9px;
  color: #40506b;
  font-size: 16px;
  font-weight: 500;
}

.verify-upload em {
  display: block;
  margin-bottom: 20px;
  color: #75849b;
  font-size: 14px;
  font-style: normal;
}

:deep(.verify-upload .el-button) {
  min-width: 176px;
  height: 44px;
  border-radius: 7px;
  font-size: 15px;
  font-weight: 600;
}

.upload-record-card {
  padding: 24px 28px;
}

.record-title {
  margin-bottom: 14px;
}

.upload-table-wrap {
  overflow-x: auto;
  border: 1px solid #e3eaf5;
  border-radius: 9px;
}

.upload-table {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;
  color: #263853;
  font-size: 15px;
}

.upload-table th {
  height: 54px;
  padding: 0 16px;
  color: #566681;
  background: #f8fbff;
  font-weight: 700;
  text-align: left;
}

.upload-table td {
  height: 54px;
  padding: 0 16px;
  border-top: 1px solid #e6edf6;
  background: #ffffff;
}

.upload-table tbody tr {
  cursor: pointer;
}

.upload-table tbody tr:hover td,
.upload-table tbody tr.active td {
  background: #fbfdff;
}

.select-radio {
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #b8c7dc;
  border-radius: 50%;
  background: #ffffff;
  cursor: pointer;
}

.select-radio.checked {
  border-color: #2f7cff;
  background: #2f7cff;
}

.select-radio.checked::after {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffffff;
  content: "";
}

.file-cell {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #263853;
}

.file-cell i {
  display: inline-flex;
  width: 18px;
  height: 21px;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  color: #ffffff;
  background: #f23838;
  font-size: 6px;
  font-style: normal;
  font-weight: 800;
}

.preview-button {
  padding: 0;
  border: 0;
  color: #1677ff;
  background: transparent;
  font-size: 15px;
  cursor: pointer;
}

.record-hint {
  margin: 16px 0 20px;
  color: #74839a;
  font-size: 15px;
}

.record-hint::before {
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  border: 1px solid #aebcd0;
  border-radius: 50%;
  color: #8796ab;
  content: "i";
  font-size: 11px;
  font-weight: 700;
}

.verify-submit {
  width: 128px;
  height: 46px;
  border-radius: 7px;
  font-size: 16px;
  font-weight: 600;
}

.result-box {
  margin-top: 24px;
  padding: 20px;
  border-radius: 12px;
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

@media (max-width: 900px) {
  .verify-head {
    min-height: 190px;
  }

  .verify-head-copy {
    padding: 30px 24px;
  }

  .verify-head h2 {
    font-size: 34px;
  }

  .compose-grid {
    grid-template-columns: 1fr;
  }

  .contract-section {
    padding-right: 0;
    padding-bottom: 22px;
    border-right: 0;
    border-bottom: 1px solid #dce5f2;
  }
}
</style>
