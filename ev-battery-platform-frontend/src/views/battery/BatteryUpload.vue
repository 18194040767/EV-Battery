<template>
  <div>
    <el-upload :auto-upload="false" :show-file-list="false" :on-change="single">
      <el-button type="primary">上传CSV/Excel（单个）</el-button>
    </el-upload>
    <el-upload :auto-upload="false" multiple :show-file-list="false" :on-change="batch" style="margin-top: 10px;">
      <el-button>批量上传</el-button>
    </el-upload>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadBatch, uploadSingle } from '../../api/battery'
import { extractErrorMessage } from '../../utils/requestError'

const router = useRouter()
const single = async (file) => {
  try {
    const form = new FormData()
    form.append('file', file.raw)
    const res = await uploadSingle(form)
    const warnings = res?.data?.warnings || []
    ElMessage.success(warnings.length ? `上传成功，但需补充: ${warnings.join('；')}` : '上传成功')
    await router.push('/battery/list')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '上传失败'))
  }
}
const batch = async (file, fileList) => {
  try {
    const form = new FormData()
    fileList.forEach((f) => form.append('files', f.raw))
    const res = await uploadBatch(form)
    const failCount = Number(res?.data?.failCount || 0)
    ElMessage.success(failCount ? `批量导入完成，成功 ${res.data.successCount} 个，失败 ${failCount} 个` : '批量上传成功')
    await router.push('/battery/list')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '批量上传失败'))
  }
}
</script>
