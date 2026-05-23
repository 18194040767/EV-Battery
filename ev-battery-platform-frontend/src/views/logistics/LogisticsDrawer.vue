<template>
  <el-dialog v-model="visible" title="填写物流信息" width="560px" class="logistics-dialog">
    <div class="notice-card">
      <strong>危险品运输告知单</strong>
      <p>提交运单号后，系统会同步生成锂电池危险品运输告知单 PDF，并自动开始下载。</p>
    </div>
    <el-form :model="form" label-position="top">
      <el-form-item label="物流公司">
        <el-select v-model="form.company" placeholder="请选择物流公司">
          <el-option label="顺丰速运" value="顺丰速运" />
          <el-option label="京东物流" value="京东物流" />
          <el-option label="德邦快递" value="德邦快递" />
          <el-option label="圆通速递" value="圆通速递" />
        </el-select>
      </el-form-item>
      <el-form-item label="运单号">
        <el-input v-model="form.trackingNo" placeholder="请输入运单号" />
      </el-form-item>
      <el-form-item label="紧急联系人">
        <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
      </el-form-item>
      <el-form-item label="紧急联系电话">
        <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, computed, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadHazardousNotice, fillTracking } from '../../api/logistics'
import { downloadPdfResponse, getDownloadErrorMessage } from '../../utils/file'

const props = defineProps({
  modelValue: Boolean,
  orderId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const submitting = ref(false)
const form = reactive({
  company: '顺丰速运',
  trackingNo: '',
  contactName: '平台应急联系人',
  contactPhone: '400-800-1234'
})

const downloadNotice = async (orderId, trackingNo) => {
  const res = await downloadHazardousNotice(orderId)
  await downloadPdfResponse(res, `危险品运输告知单-${trackingNo || orderId}.pdf`)
}

watch(() => props.modelValue, (value) => {
  if (value) {
    form.company = '顺丰速运'
    form.trackingNo = `SF${Date.now()}`
    form.contactName = '平台应急联系人'
    form.contactPhone = '400-800-1234'
  }
})

const submit = async () => {
  if (!props.orderId) {
    ElMessage.warning('缺少订单编号')
    return
  }
  if (!form.company || !form.trackingNo) {
    ElMessage.warning('请填写物流公司和运单号')
    return
  }

  submitting.value = true
  try {
    const res = await fillTracking({ orderId: props.orderId, ...form })
    await downloadNotice(props.orderId, form.trackingNo)
    ElMessage.success('物流信息已提交，告知单 PDF 已生成')
    emit('success', res?.data || {})
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(await getDownloadErrorMessage(error, '物流信息提交或告知单下载失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.notice-card {
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(47, 124, 255, 0.18);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(47, 124, 255, 0.09), rgba(20, 184, 166, 0.08));
}

.notice-card strong {
  display: block;
  margin-bottom: 6px;
  color: #101a33;
}

.notice-card p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.7;
}

:deep(.el-select),
:deep(.el-input) {
  width: 100%;
}
</style>
