<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const site = ref(null)

onMounted(async () => {
  try {
    site.value = await api.getSite()
  } catch {
    /* 站点设置不可用时降级 */
  }
})
</script>

<template>
  <div>
    <div class="announcement">
      运动玩具新系列已上线
      <router-link to="/products">立即探索 ↗</router-link>
    </div>
    <header class="site-header">
      <div class="inner">
        <router-link to="/" class="brand">
          <span class="logo-mark"><img src="/wemove-logo.png" alt="WEMOVE" /></span>
          <span>
            {{ site?.brand_name || 'WEMOVE' }}
            <small>运动 · 玩乐 · 成长</small>
          </span>
        </router-link>
        <nav class="site-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/products">产品</router-link>
          <router-link to="/play">玩乐指南</router-link>
          <router-link to="/about">品牌故事</router-link>
          <router-link to="/support">帮助中心</router-link>
          <router-link to="/contact">联系我们</router-link>
          <router-link to="/dealers/login" class="btn-nav">经销商登录</router-link>
        </nav>
      </div>
    </header>
  </div>
</template>
