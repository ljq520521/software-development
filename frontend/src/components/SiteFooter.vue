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
    <div class="inner">
      <div>
        <h4>{{ site?.brand_name || 'WEMOVE SPORTS' }}</h4>
        <p style="font-size: 13.5px; line-height: 1.7; margin: 0;">{{ site?.tagline || 'Sport toys for active families.' }}</p>
        <p style="font-size: 13px; margin: 12px 0 0;">
          <a v-if="site?.contact_email" :href="`mailto:${site.contact_email}`">{{ site.contact_email }}</a>
          <span v-if="site?.contact_phone" style="margin-left: 12px;">{{ site.contact_phone }}</span>
        </p>
      </div>
      <div>
        <h4>Explore</h4>
        <router-link to="/products">Products</router-link>
        <router-link to="/play">Play &amp; Learn</router-link>
        <router-link to="/support">Support</router-link>
        <router-link to="/contact">Contact</router-link>
      </div>
      <div>
        <h4>Company</h4>
        <router-link to="/about">About</router-link>
        <router-link to="/quality-safety">Quality &amp; Safety</router-link>
        <router-link to="/dealers/apply">Become a Dealer</router-link>
        <router-link to="/dealers/login">Dealer Sign In</router-link>
        <router-link to="/privacy">Privacy</router-link>
        <router-link to="/terms">Terms</router-link>
      </div>
    </div>
    <div class="copyright">
      © {{ new Date().getFullYear() }} {{ site?.brand_name || 'WEMOVE SPORTS' }}. All rights reserved.
    </div>
  </footer>
</template>
