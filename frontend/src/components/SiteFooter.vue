<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const site = ref(null)

onMounted(async () => {
  try {
    site.value = await api.getSite()
  } catch {
    /* ignore */
  }
})
</script>

<template>
  <footer class="site-footer">
    <div class="footer-main">
      <div>
        <router-link to="/" class="brand light" style="color: #fff;">
          <span class="logo-mark"><img src="/wemove-logo.png" alt="WEMOVE" /></span>
          <span>
            {{ site?.brand_name || 'WEMOVE' }}
            <small style="color: rgba(255,255,255,.55);">运动 · 玩乐 · 成长</small>
          </span>
        </router-link>
        <p>{{ site?.tagline || '让每一次动起来都充满乐趣。' }}</p>
        <p v-if="site?.contact_email">
          邮箱:<a :href="`mailto:${site.contact_email}`" style="color: var(--wemove-yellow);">{{ site.contact_email }}</a>
        </p>
        <p v-if="site?.contact_phone">电话:{{ site.contact_phone }}</p>
      </div>
      <div>
        <h3>探索</h3>
        <router-link to="/products">全部产品</router-link>
        <router-link to="/play">玩乐指南</router-link>
        <router-link to="/support">帮助中心</router-link>
        <router-link to="/orders/search">我的订单</router-link>
        <router-link to="/contact">联系我们</router-link>
      </div>
      <div>
        <h3>品牌</h3>
        <router-link to="/about">品牌故事</router-link>
        <router-link to="/quality-safety">质量与安全</router-link>
        <router-link to="/dealers/apply">经销商合作</router-link>
        <router-link to="/dealers/login">经销商登录</router-link>
      </div>
      <div>
        <h3>法律</h3>
        <router-link to="/privacy">隐私政策</router-link>
        <router-link to="/terms">服务条款</router-link>
      </div>
    </div>
    <div class="footer-bottom">
      <span>© {{ new Date().getFullYear() }} {{ site?.brand_name || 'WEMOVE' }} 保留所有权利</span>
      <span>一起动起来,一起玩出精彩</span>
    </div>
  </footer>
</template>
