<template>
  <el-dialog v-model="visible" title="填写物流信息" width="520px">
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
import { fillTracking } from '../../api/logistics'

const props = defineProps({
  modelValue: Boolean,
  orderId: {
    type: Number,
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

  submitting.value = true
  try {
    const res = await fillTracking({ orderId: props.orderId, ...form })
    ElMessage.success('物流信息已提交')
    emit('success', res?.data || {})
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>
