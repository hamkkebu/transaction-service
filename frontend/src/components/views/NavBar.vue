<template>
  <nav class="navbar">
    <div class="navbar-container">
      <div class="navbar-brand">
        <router-link :to="ROUTES.HOME" class="brand-link">
          <div class="brand-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
              <path d="M2 17l10 5 10-5"></path>
              <path d="M2 12l10 5 10-5"></path>
            </svg>
          </div>
          <span class="brand-name">Auth Service</span>
        </router-link>
      </div>

      <div class="navbar-menu" :class="{ 'is-active': menuActive }">
        <div class="navbar-start">
          <button
            v-if="!isAuthenticated"
            class="navbar-item"
            @click="handleRegister"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="8.5" cy="7" r="4"></circle>
              <line x1="20" y1="8" x2="20" y2="14"></line>
              <line x1="23" y1="11" x2="17" y2="11"></line>
            </svg>
            회원가입
          </button>

          <router-link
            v-if="isAuthenticated"
            :to="ROUTES.USER_INFO"
            class="navbar-item"
            @click="closeMenu"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            내 정보
          </router-link>

          <router-link
            v-if="isAuthenticated && isAdmin"
            :to="ROUTES.ADMIN_DASHBOARD"
            class="navbar-item admin-link"
            @click="closeMenu"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            관리자
          </router-link>
        </div>

        <div class="navbar-end">
          <div v-if="isAuthenticated" class="navbar-user">
            <div class="user-info">
              <div class="user-avatar">
                {{ getInitials(currentUser?.firstName, currentUser?.lastName) }}
              </div>
              <div class="user-details">
                <span class="user-name">{{ currentUser?.firstName }} {{ currentUser?.lastName }}</span>
                <span class="user-role" :class="currentUser?.role?.toLowerCase()">
                  {{ getRoleLabel(currentUser?.role) }}
                </span>
              </div>
            </div>
            <button class="btn-logout" @click="handleLogout">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
              로그아웃
            </button>
          </div>

          <button
            v-else
            class="btn-login"
            @click="handleLogin"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
              <polyline points="10 17 15 12 10 7"></polyline>
              <line x1="15" y1="12" x2="3" y2="12"></line>
            </svg>
            로그인
          </button>
        </div>
      </div>

      <button class="navbar-burger" @click="toggleMenu" :class="{ 'is-active': menuActive }">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>
  </nav>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue';
import { useAuth } from '@/composables/useAuth';
import { ROUTES } from '@/constants';
import type { UserRole } from '@/types/domain.types';

export default defineComponent({
  name: 'NavBar',
  setup() {
    const { currentUser, isAuthenticated, logout, login, register } = useAuth();
    const menuActive = ref(false);

    const isAdmin = computed(() => {
      const role = currentUser.value?.role;
      return role === 'ADMIN' || role === 'DEVELOPER';
    });

    const toggleMenu = () => {
      menuActive.value = !menuActive.value;
    };

    const closeMenu = () => {
      menuActive.value = false;
    };

    const handleLogout = async () => {
      if (confirm('로그아웃 하시겠습니까?')) {
        closeMenu();
        // Keycloak SSO 로그아웃 - ledger-service 홈으로 리다이렉트
        await logout('http://localhost:3002/home');
      }
    };

    const handleLogin = () => {
      closeMenu();
      // Keycloak SSO 로그인 페이지로 리다이렉트
      login();
    };

    const handleRegister = () => {
      closeMenu();
      // Keycloak SSO 회원가입 페이지로 리다이렉트
      register();
    };

    const getRoleLabel = (role?: UserRole): string => {
      if (!role) return '';
      const labels: Record<UserRole, string> = {
        USER: '사용자',
        ADMIN: '관리자',
        DEVELOPER: '개발자',
      };
      return labels[role] || role;
    };

    const getInitials = (firstName?: string, lastName?: string): string => {
      const first = firstName?.charAt(0) || '';
      const last = lastName?.charAt(0) || '';
      return (first + last).toUpperCase() || 'U';
    };

    return {
      ROUTES,
      currentUser,
      isAuthenticated,
      isAdmin,
      menuActive,
      toggleMenu,
      closeMenu,
      handleLogout,
      handleLogin,
      handleRegister,
      getRoleLabel,
      getInitials,
    };
  },
});
</script>

<style scoped>
.navbar {
  background: rgba(10, 10, 15, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
}

.navbar-brand {
  flex-shrink: 0;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  transition: all 0.3s;
}

.brand-link:hover {
  transform: translateY(-1px);
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
}

.brand-link:hover .brand-icon {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transform: rotate(-5deg);
}

.brand-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.brand-name {
  font-family: 'Space Grotesk', sans-serif;
  font-weight: 700;
  font-size: 18px;
  background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.7) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
  justify-content: space-between;
  margin-left: 48px;
}

.navbar-start,
.navbar-end {
  display: flex;
  align-items: center;
  gap: 8px;
}

.navbar-item {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-weight: 500;
  font-size: 14px;
  padding: 10px 16px;
  border-radius: 10px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.navbar-item svg {
  width: 18px;
  height: 18px;
  opacity: 0.7;
  transition: opacity 0.3s;
}

.navbar-item:hover {
  color: rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.05);
}

.navbar-item:hover svg {
  opacity: 1;
}

.navbar-item.router-link-active {
  color: rgba(255, 255, 255, 0.95);
  background: rgba(102, 126, 234, 0.15);
}

.navbar-item.router-link-active::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 1px;
}

.navbar-item.admin-link {
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
  color: #a855f7;
}

.navbar-item.admin-link svg {
  opacity: 1;
  color: #a855f7;
}

.navbar-item.admin-link:hover {
  background: rgba(168, 85, 247, 0.2);
  border-color: rgba(168, 85, 247, 0.3);
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: white;
  letter-spacing: 0.02em;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 600;
  font-size: 14px;
  line-height: 1.2;
}

.user-role {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
  width: fit-content;
}

.user-role.admin {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.user-role.developer {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-logout svg {
  width: 16px;
  height: 16px;
}

.btn-logout:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.btn-login {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.btn-login svg {
  width: 16px;
  height: 16px;
}

.btn-login::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.btn-login:hover::before {
  opacity: 1;
}

.btn-login:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.navbar-burger {
  display: none;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  cursor: pointer;
  padding: 8px;
  width: 40px;
  height: 40px;
  position: relative;
  transition: all 0.3s;
}

.navbar-burger:hover {
  background: rgba(255, 255, 255, 0.1);
}

.navbar-burger span {
  display: block;
  height: 2px;
  width: 20px;
  background: rgba(255, 255, 255, 0.8);
  margin: 4px auto;
  transition: all 0.3s;
  border-radius: 1px;
}

.navbar-burger.is-active span:nth-child(1) {
  transform: translateY(6px) rotate(45deg);
}

.navbar-burger.is-active span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}

.navbar-burger.is-active span:nth-child(3) {
  transform: translateY(-6px) rotate(-45deg);
}

@media (max-width: 768px) {
  .navbar-burger {
    display: block;
  }

  .navbar-menu {
    display: none;
    position: absolute;
    top: 64px;
    left: 0;
    right: 0;
    background: rgba(10, 10, 15, 0.95);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    margin: 0;
    padding: 16px;
  }

  .navbar-menu.is-active {
    display: flex;
    animation: slideDown 0.3s ease-out;
  }

  @keyframes slideDown {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .navbar-start,
  .navbar-end {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    width: 100%;
  }

  .navbar-start {
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    margin-bottom: 16px;
  }

  .navbar-item {
    width: 100%;
    justify-content: center;
    padding: 12px 16px;
  }

  .navbar-item.router-link-active::before {
    display: none;
  }

  .navbar-user {
    flex-direction: column;
    gap: 16px;
    padding: 8px 0;
  }

  .user-info {
    justify-content: center;
  }

  .btn-logout,
  .btn-login {
    width: 100%;
    justify-content: center;
  }
}
</style>
