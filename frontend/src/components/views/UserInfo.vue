<template>
  <div class="userinfo-container">
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="userinfo-card">
      <div class="userinfo-brand">
        <div class="brand-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
      </div>

      <div class="userinfo-header">
        <h1 class="userinfo-title">My Profile</h1>
        <p class="userinfo-subtitle">내 정보를 확인하세요</p>
      </div>

      <div class="action-bar">
        <div class="user-count">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          <span>회원 정보</span>
        </div>
        <button @click="downloadExcel()" type="button" class="btn-download">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>엑셀 다운로드</span>
        </button>
      </div>

      <div class="table-container" v-if="result.length > 0">
        <div class="user-card" v-for="(user, index) in result" :key="index">
          <div class="user-card-header">
            <div class="user-avatar">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="user-basic-info">
              <h3>{{ user.firstName }} {{ user.lastName }}</h3>
              <p>@{{ user.nickname }}</p>
            </div>
            <div class="user-id-badge">
              ID: {{ user.username }}
            </div>
          </div>

          <div class="user-card-body">
            <div class="info-grid">
              <div class="info-item">
                <svg class="info-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
                <div>
                  <span class="info-label">이메일</span>
                  <span class="info-value">{{ user.email || 'N/A' }}</span>
                </div>
              </div>

              <div class="info-item">
                <svg class="info-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                </svg>
                <div>
                  <span class="info-label">전화번호</span>
                  <span class="info-value">{{ user.phone || 'N/A' }}</span>
                </div>
              </div>

              <div class="info-item full-width">
                <svg class="info-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                </svg>
                <div>
                  <span class="info-label">주소</span>
                  <span class="info-value">
                    {{ [user.street1, user.street2, user.city, user.state, user.country, user.zip].filter(Boolean).join(', ') || 'N/A' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" v-else>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        <h3>등록된 회원이 없습니다</h3>
        <p>새로운 회원을 등록해주세요.</p>
      </div>

      <div class="service-link">
        <a :href="ledgerServiceUrl" class="btn-ledger">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            <line x1="12" y1="6" x2="12" y2="12"></line>
            <line x1="9" y1="9" x2="15" y2="9"></line>
          </svg>
          <div class="btn-ledger-content">
            <span class="btn-ledger-title">가계부 서비스</span>
            <span class="btn-ledger-subtitle">수입/지출을 관리해보세요</span>
          </div>
          <svg class="arrow-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </a>
      </div>

      <div class="navigation-buttons">
        <button @click="goToLogout" class="btn-secondary">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          <span>로그아웃</span>
        </button>
        <button @click="goToLeave" class="btn-danger">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
          <span>회원탈퇴</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import * as XLSX from 'xlsx';
import apiClient from '@/api/client';
import { useAuth } from '@/composables/useAuth';
import { useApi } from '@/composables/useApi';
import { API_ENDPOINTS, ROUTES } from '@/constants';
import { formatDate } from '@/utils/date.utils';
import type { Sample } from '@/types/domain.types';

export default defineComponent({
  name: 'UserInfo',
  setup() {
    const router = useRouter();
    const { currentUser, logout, login } = useAuth();
    const { loading, execute } = useApi<Sample>();
    const result = ref<Sample[]>([]);

    // Ledger Service URL (SSO: 토큰 전달 불필요, Keycloak 세션으로 자동 인증)
    const ledgerServiceUrl = process.env.VUE_APP_LEDGER_SERVICE_URL || 'http://localhost:3002/dashboard';

    const getUserInfo = async () => {
      if (!currentUser.value?.username) {
        alert('로그인 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
        login();
        return;
      }

      try {
        const data = await execute(() =>
          apiClient.get(API_ENDPOINTS.USER_BY_USERNAME(currentUser.value!.username))
        );

        if (data) {
          result.value = [data];
        }
      } catch (error: any) {
        // 사용자를 찾을 수 없는 경우 (USER-101 또는 404 에러)
        const errorCode = error?.response?.data?.error?.code;
        const errorMessage = error?.response?.data?.error?.message || '';

        if (errorCode === 'USER-101' || errorMessage.includes('사용자를 찾을 수 없습니다')) {
          alert('사용자를 찾을 수 없습니다. 로그인 화면으로 되돌아갑니다.');
          logout();
        }
      }
    };

    const downloadExcel = () => {
      if (result.value.length === 0) {
        alert('다운로드할 데이터가 없습니다.');
        return;
      }

      const dataWS = XLSX.utils.json_to_sheet(result.value);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, dataWS, 'Sheet1');
      const filename = `my_account_info_${formatDate(new Date().toISOString(), 'YYYYMMDD_HHmmss')}.xlsx`;
      XLSX.writeFile(wb, filename);
    };

    const goToLogout = () => {
      logout();
    };

    const goToLeave = () => {
      router.push(ROUTES.LEAVE_USER);
    };

    onMounted(() => {
      getUserInfo();
    });

    return {
      result,
      loading,
      getUserInfo,
      downloadExcel,
      goToLogout,
      goToLeave,
      ledgerServiceUrl,
    };
  },
});
</script>

<style scoped>
.userinfo-container {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 20s ease-in-out infinite;
}

.orb-1 { width: 500px; height: 500px; background: linear-gradient(135deg, #667eea, #764ba2); top: -150px; right: -150px; }
.orb-2 { width: 400px; height: 400px; background: linear-gradient(135deg, #11998e, #38ef7d); bottom: -100px; left: -100px; animation-delay: -7s; }
.orb-3 { width: 300px; height: 300px; background: linear-gradient(135deg, #f093fb, #f5576c); top: 40%; left: 30%; animation-delay: -14s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 30px) scale(0.95); }
}

.userinfo-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 40px;
  width: 100%;
  max-width: 600px;
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.userinfo-brand { display: flex; justify-content: center; margin-bottom: 20px; }

.brand-icon {
  width: 52px; height: 52px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
}

.brand-icon svg { width: 26px; height: 26px; color: white; }

.userinfo-header { text-align: center; margin-bottom: 28px; }

.userinfo-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 28px; font-weight: 700;
  background: linear-gradient(135deg, #fff, rgba(255,255,255,0.7));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; margin: 0 0 8px 0;
}

.userinfo-subtitle { color: rgba(255, 255, 255, 0.5); font-size: 14px; margin: 0; }

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.user-count {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 500;
}

.user-count svg { width: 20px; height: 20px; color: #667eea; }

.btn-download {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-download svg { width: 16px; height: 16px; }

.btn-download:hover {
  background: rgba(16, 185, 129, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.table-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.user-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s;
}

.user-card:hover {
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.15);
}

.user-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar svg { width: 28px; height: 28px; color: white; }

.user-basic-info { flex: 1; }

.user-basic-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.95);
  font-weight: 600;
}

.user-basic-info p {
  margin: 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.user-id-badge {
  padding: 6px 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.user-card-body { padding: 20px; }

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  transition: transform 0.3s;
}

.info-item:hover { transform: translateY(-2px); }

.info-item.full-width { grid-column: 1 / -1; }

.info-icon {
  width: 18px;
  height: 18px;
  color: #667eea;
  flex-shrink: 0;
  margin-top: 2px;
}

.info-item > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.info-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  word-break: break-word;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 8px 0;
}

.empty-state p { font-size: 14px; margin: 0; }

.service-link {
  margin-top: 24px;
  margin-bottom: 16px;
}

.btn-ledger {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 14px;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-ledger:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.25), rgba(118, 75, 162, 0.25));
  border-color: rgba(102, 126, 234, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
}

.btn-ledger > svg:first-child {
  width: 32px;
  height: 32px;
  color: #667eea;
  flex-shrink: 0;
}

.btn-ledger-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.btn-ledger-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
}

.btn-ledger-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.arrow-icon {
  width: 20px;
  height: 20px;
  color: rgba(255, 255, 255, 0.4);
  transition: transform 0.3s, color 0.3s;
}

.btn-ledger:hover .arrow-icon {
  color: #667eea;
  transform: translateX(4px);
}

.navigation-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.btn-secondary,
.btn-danger {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-secondary svg,
.btn-danger svg { width: 18px; height: 18px; }

.btn-secondary {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-secondary:hover {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
  color: #667eea;
  transform: translateY(-2px);
}

.btn-danger {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.btn-danger:hover {
  background: rgba(239, 68, 68, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

@media (max-width: 640px) {
  .userinfo-card { padding: 28px 20px; }
  .userinfo-title { font-size: 24px; }
  .action-bar { flex-direction: column; gap: 12px; align-items: stretch; }
  .info-grid { grid-template-columns: 1fr; }
  .user-card-header { flex-wrap: wrap; }
  .user-id-badge { width: 100%; text-align: center; margin-top: 8px; }
  .navigation-buttons { flex-direction: column; }
  .orb-1 { width: 300px; height: 300px; }
  .orb-2 { width: 250px; height: 250px; }
  .orb-3 { width: 180px; height: 180px; }
}
</style>
