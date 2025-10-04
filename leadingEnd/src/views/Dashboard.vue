<template>
  <div class="dashboard">
    <h1>油气管道监控系统仪表盘</h1>
    <p>欢迎，{{ adminTypeText }}！</p>
    <button @click="logout">退出登录</button>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'DashboardView',
  computed: {
    ...mapGetters(['adminType']),
    adminTypeText() {
      return this.adminType === 'global' ? '全局管理员' : '区域管理员'
    }
  },
  methods: {
    logout() {
      this.$store.dispatch('logout')
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
</style>