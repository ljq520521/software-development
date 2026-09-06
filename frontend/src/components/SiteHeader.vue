<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const site = ref(null)

onMounted(async () => {
  try {
    site.value = await api.getSite()
  } catch {
    /* 站点设置不可用时降级为默认品牌名 */
  }
})
</script>

<template>
  <header class="site-header">
    <div class="inner">
      <router-link to="/" class="brand">
        <span class="logo-mark">W</span>
        <span>{{ site?.brand_name || 'WEMOVE SPORTS' }}</span>
      </router-link>
      <nav class="site-nav">
        <router-link to="/">Home</router-link>
        <router-link to="/products">Products</router-link>
        <router-link to="/play">Play &amp; Learn</router-link>
        <router-link to="/about">About</router-link>
        <router-link to="/support">Support</router-link>
        <router-link to="/contact">Contact</router-link>
        <router-link to="/dealers/login" class="btn-outline" style="padding: 6px 14px; border-color: rgba(255,255,255,.5); color:#fff;">Dealer Sign In</router-link>
        <router-link to="/dealers/apply" class="btn-outline" style="padding: 6px 14px; border-color: rgba(255,255,255,.5); color:#fff;">Become a Dealer</router-link>
      </nav>
    </div>
  </header>
</template>
