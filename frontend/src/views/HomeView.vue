<template>
  <div class="page">
    <header class="topbar">
      <h2>Главная</h2>
      <button @click="logout" class="logout">Выйти</button>
    </header>
    <p>Тут появится таблица с фильтрами, сортировкой и пагинацией.</p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAuth } from '../composables/useAuth';

const { logout: doLogout } = useAuth();
const router = useRouter();

function logout() {
  doLogout();
  router.push({ name: 'auth' });
}
</script>

<style scoped>
.page { padding: 24px; }
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.logout {
  background: white; border: 1px solid #cfe0ff; color: #1f56ff;
  border-radius: 8px; padding: 8px 12px; cursor: pointer;
}
</style>
