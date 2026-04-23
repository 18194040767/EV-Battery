<template>
  <div v-if="profile.id" class="page-shell">
    <section class="panel-card profile-head">
      <div class="user-box">
        <el-avatar :size="72" :src="profile.avatar">{{ (profile.nickname || profile.username || 'U').slice(0, 1) }}</el-avatar>
        <div>
          <h2>{{ profile.nickname || profile.username }}</h2>
          <p>{{ profile.bio || '暂无简介' }}</p>
          <span>信用分 {{ profile.creditScore || 100 }} · 收藏 {{ profile.favorites || 0 }} · 购物车 {{ profile.cartCount || 0 }}</span>
        </div>
      </div>
      <el-button v-if="isSelf" @click="editVisible = true">编辑资料</el-button>
    </section>

    <section class="panel-card profile-tabs">
      <el-tabs>
        <el-tab-pane label="在售商品">
          <div class="product-grid">
            <article v-for="item in profile.products || []" :key="item.id" class="product-card">
              <img :src="item.coverImage || placeholder" alt="" class="cover" @click="$router.push('/trade/product/' + item.id)" />
              <strong>{{ item.title }}</strong>
              <p>¥{{ item.price }} · {{ item.publishStatus }}</p>
            </article>
          </div>
        </el-tab-pane>
        <el-tab-pane label="收到的评价">
          <el-empty v-if="!(profile.receivedReviews || []).length" description="暂无评价记录" />
          <article v-for="item in profile.receivedReviews || []" :key="item.id" class="review-card">
            <strong>{{ item.content || '该买家未填写文字评价。' }}</strong>
            <span>评分 {{ item.score }}</span>
          </article>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="editVisible" title="编辑资料" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="头像地址"><el-input v-model="form.avatar" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.bio" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../store/user'
import { getTradeProfile, updateTradeProfile } from '../../api/trade'

const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=User'
const route = useRoute()
const userStore = useUserStore()
const profile = ref({})
const editVisible = ref(false)
const form = reactive({ nickname: '', avatar: '', city: '', bio: '' })

const isSelf = computed(() => !route.params.id || Number(route.params.id) === Number(userStore.userId))

const load = async () => {
  const res = await getTradeProfile(route.params.id)
  profile.value = res?.data || {}
  Object.assign(form, profile.value)
}

const save = async () => {
  await updateTradeProfile(form)
  editVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.profile-head,
.profile-tabs {
  padding: 24px;
}

.profile-head,
.user-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.profile-head h2 {
  margin: 0 0 8px;
}

.profile-head p,
.profile-head span,
.product-card p,
.review-card span {
  color: var(--app-muted);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card,
.review-card {
  padding: 18px;
  border-radius: 18px;
  background: #f8fbfa;
}

.cover {
  width: 100%;
  height: 200px;
  border-radius: 18px;
  object-fit: cover;
  cursor: pointer;
  margin-bottom: 12px;
}

.review-card {
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .profile-head,
  .user-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>

