<script setup lang="ts">
defineOptions({
  name: 'LoginIndex',
})
</script>

<template>
  <div class="login-layout">
    <div class="login-container">
      <div class="login-header">
        <h1>管理端部署工具</h1>
      </div>

      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>

      <footer class="login-footer">
        <p>© {{ new Date().getFullYear() }} All Rights Reserved.</p>
      </footer>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-layout {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
}

.login-container {
  width: 460px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  text-align: center;
  transition: 0.3s;
  padding: 20px;

  &:hover,
  &:focus-within {
    box-shadow: 0px 0px 12px rgba(0, 0, 0, 0.12);
  }
}

.login-header h1 {
  font-size: 26px;
  font-weight: 500;
  color: #303133;
  margin-top: 10px;
  margin-bottom: 20px;
}

.login-footer {
  margin-top: 30px;
  padding-top: 20px;
  font-size: 12px;
  color: #888;
  border-top: 1px solid #e0e0e0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
