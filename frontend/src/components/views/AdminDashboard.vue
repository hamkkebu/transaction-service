<template>
  <div class="admin-container">
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="dashboard-content">
      <div class="dashboard-header">
        <div class="header-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="7" height="7"></rect>
            <rect x="14" y="3" width="7" height="7"></rect>
            <rect x="14" y="14" width="7" height="7"></rect>
            <rect x="3" y="14" width="7" height="7"></rect>
          </svg>
        </div>
        <div class="header-text">
          <h1 class="dashboard-title">관리자 대시보드</h1>
          <p class="dashboard-subtitle">사용자 및 시스템 관리</p>
        </div>
      </div>

      <!-- 통계 카드 -->
      <div class="stats-grid" v-if="stats">
        <div class="stat-card">
          <div class="stat-icon users">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">전체 사용자</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon active">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.activeUsers }}</div>
            <div class="stat-label">활성 사용자</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon admin">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
              <path d="M2 17l10 5 10-5"></path>
              <path d="M2 12l10 5 10-5"></path>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.adminUsers + stats.developerUsers }}</div>
            <div class="stat-label">관리자</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon deleted">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.deletedUsers }}</div>
            <div class="stat-label">탈퇴 사용자</div>
          </div>
        </div>
      </div>

      <!-- 탭 메뉴 -->
      <div class="tabs">
        <button
          class="tab"
          :class="{ active: activeTab === 'users' }"
          @click="activeTab = 'users'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
          </svg>
          활성 사용자
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'deleted' }"
          @click="activeTab = 'deleted'; loadDeletedUsers()"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
          탈퇴 사용자
        </button>
      </div>

      <!-- 사용자 목록 -->
      <div class="table-card">
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>로딩 중...</p>
        </div>

        <div v-else-if="error" class="error-message">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          {{ error }}
        </div>

        <div v-else class="table-wrapper">
          <table class="user-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>아이디</th>
                <th>이름</th>
                <th>이메일</th>
                <th>권한</th>
                <th>상태</th>
                <th>가입일</th>
                <th v-if="activeTab === 'users'">작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in currentUsers" :key="user.userId">
                <td class="td-id">{{ user.userId }}</td>
                <td class="td-username">{{ user.username }}</td>
                <td>{{ user.firstName }} {{ user.lastName }}</td>
                <td class="td-email">{{ user.email }}</td>
                <td>
                  <span class="role-badge" :class="user.role.toLowerCase()">
                    {{ getRoleLabel(user.role) }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="user.isActive ? 'active' : 'inactive'">
                    {{ user.isActive ? '활성' : '비활성' }}
                  </span>
                </td>
                <td class="td-date">{{ formatDate(user.createdAt) }}</td>
                <td v-if="activeTab === 'users'" class="td-actions">
                  <button
                    class="btn-action role"
                    @click="openRoleModal(user)"
                    :disabled="user.username === currentUser?.username"
                    title="권한 변경"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                      <path d="M2 17l10 5 10-5"></path>
                      <path d="M2 12l10 5 10-5"></path>
                    </svg>
                  </button>
                  <button
                    class="btn-action"
                    :class="user.isActive ? 'danger' : 'success'"
                    @click="toggleUserStatus(user)"
                    :disabled="user.username === currentUser?.username"
                    :title="user.isActive ? '비활성화' : '활성화'"
                  >
                    <svg v-if="user.isActive" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"></circle>
                      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                      <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 권한 변경 모달 -->
    <div v-if="showRoleModal" class="modal-overlay" @click="closeRoleModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>권한 변경</h2>
          <button class="modal-close" @click="closeRoleModal">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p class="modal-desc">
            <strong>{{ selectedUser?.username }}</strong> 사용자의 권한을 변경합니다.
          </p>
          <div class="role-options">
            <label class="role-option" :class="{ selected: newRole === 'USER' }">
              <input type="radio" v-model="newRole" value="USER" />
              <div class="role-option-content">
                <div class="role-option-icon user">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
                <div>
                  <div class="role-option-title">일반 사용자</div>
                  <div class="role-option-desc">기본 사용자 권한</div>
                </div>
              </div>
            </label>
            <label class="role-option" :class="{ selected: newRole === 'DEVELOPER' }">
              <input type="radio" v-model="newRole" value="DEVELOPER" />
              <div class="role-option-content">
                <div class="role-option-icon developer">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="16 18 22 12 16 6"></polyline>
                    <polyline points="8 6 2 12 8 18"></polyline>
                  </svg>
                </div>
                <div>
                  <div class="role-option-title">개발자</div>
                  <div class="role-option-desc">개발자 전용 기능 접근</div>
                </div>
              </div>
            </label>
            <label class="role-option" :class="{ selected: newRole === 'ADMIN' }">
              <input type="radio" v-model="newRole" value="ADMIN" />
              <div class="role-option-content">
                <div class="role-option-icon admin">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                  </svg>
                </div>
                <div>
                  <div class="role-option-title">관리자</div>
                  <div class="role-option-desc">전체 시스템 관리 권한</div>
                </div>
              </div>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeRoleModal">취소</button>
          <button class="btn-primary" @click="updateUserRole">변경</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useAuth } from '@/composables/useAuth';
import apiClient from '@/api/client';
import { API_ENDPOINTS } from '@/constants';
import type { UserResponse, UserStatsResponse, UserRole } from '@/types/domain.types';

export default defineComponent({
  name: 'AdminDashboard',
  setup() {
    const { currentUser } = useAuth();
    const loading = ref(false);
    const error = ref('');
    const activeTab = ref<'users' | 'deleted'>('users');

    const users = ref<UserResponse[]>([]);
    const deletedUsers = ref<UserResponse[]>([]);
    const stats = ref<UserStatsResponse | null>(null);

    const showRoleModal = ref(false);
    const selectedUser = ref<UserResponse | null>(null);
    const newRole = ref<UserRole>('USER');

    const currentUsers = computed(() => {
      return activeTab.value === 'users' ? users.value : deletedUsers.value;
    });

    const loadStats = async () => {
      try {
        const response = await apiClient.get(API_ENDPOINTS.ADMIN.STATS);
        stats.value = response.data.data;
      } catch (err: any) {
        console.error('Failed to load stats:', err);
      }
    };

    const loadUsers = async () => {
      loading.value = true;
      error.value = '';
      try {
        const response = await apiClient.get(API_ENDPOINTS.ADMIN.USERS);
        users.value = response.data.data;
      } catch (err: any) {
        error.value = err.response?.data?.error?.message || '사용자 목록을 불러오는데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    const loadDeletedUsers = async () => {
      if (deletedUsers.value.length > 0) return;

      loading.value = true;
      error.value = '';
      try {
        const response = await apiClient.get(API_ENDPOINTS.ADMIN.DELETED_USERS);
        deletedUsers.value = response.data.data;
      } catch (err: any) {
        error.value = err.response?.data?.error?.message || '탈퇴 사용자 목록을 불러오는데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    const openRoleModal = (user: UserResponse) => {
      selectedUser.value = user;
      newRole.value = user.role;
      showRoleModal.value = true;
    };

    const closeRoleModal = () => {
      showRoleModal.value = false;
      selectedUser.value = null;
      newRole.value = 'USER';
    };

    const updateUserRole = async () => {
      if (!selectedUser.value) return;

      try {
        await apiClient.put(
          API_ENDPOINTS.ADMIN.USER_ROLE(selectedUser.value.username),
          null,
          {
            params: { role: newRole.value }
          }
        );

        alert('권한이 변경되었습니다.');
        closeRoleModal();
        await loadUsers();
        await loadStats();
      } catch (err: any) {
        alert(err.response?.data?.error?.message || '권한 변경에 실패했습니다.');
      }
    };

    const toggleUserStatus = async (user: UserResponse) => {
      const action = user.isActive ? '비활성화' : '활성화';
      if (!confirm(`${user.username} 사용자를 ${action}하시겠습니까?`)) return;

      try {
        await apiClient.put(
          API_ENDPOINTS.ADMIN.USER_ACTIVE(user.username),
          null,
          {
            params: { isActive: !user.isActive }
          }
        );

        alert(`사용자가 ${action}되었습니다.`);
        await loadUsers();
        await loadStats();
      } catch (err: any) {
        alert(err.response?.data?.error?.message || `${action}에 실패했습니다.`);
      }
    };

    const getRoleLabel = (role: UserRole): string => {
      const labels: Record<UserRole, string> = {
        USER: '사용자',
        ADMIN: '관리자',
        DEVELOPER: '개발자',
      };
      return labels[role] || role;
    };

    const formatDate = (dateString: string): string => {
      const date = new Date(dateString);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    };

    onMounted(() => {
      loadStats();
      loadUsers();
    });

    return {
      currentUser,
      loading,
      error,
      activeTab,
      users,
      deletedUsers,
      stats,
      currentUsers,
      showRoleModal,
      selectedUser,
      newRole,
      loadDeletedUsers,
      openRoleModal,
      closeRoleModal,
      updateUserRole,
      toggleUserStatus,
      getRoleLabel,
      formatDate,
    };
  },
});
</script>

<style scoped>
.admin-container {
  min-height: calc(100vh - 64px);
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

.orb-1 { width: 500px; height: 500px; background: linear-gradient(135deg, #667eea, #764ba2); top: -150px; left: -150px; }
.orb-2 { width: 400px; height: 400px; background: linear-gradient(135deg, #a855f7, #6366f1); bottom: -100px; right: -100px; animation-delay: -7s; }
.orb-3 { width: 300px; height: 300px; background: linear-gradient(135deg, #4facfe, #00f2fe); top: 50%; left: 60%; animation-delay: -14s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 30px) scale(0.95); }
}

.dashboard-content {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.dashboard-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #a855f7, #6366f1);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(168, 85, 247, 0.3);
}

.header-icon svg {
  width: 32px;
  height: 32px;
  color: white;
}

.dashboard-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #fff, rgba(255,255,255,0.7));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 4px 0;
}

.dashboard-subtitle {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 0;
}

/* 통계 카드 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg { width: 26px; height: 26px; }

.stat-icon.users { background: rgba(102, 126, 234, 0.2); color: #667eea; }
.stat-icon.active { background: rgba(34, 197, 94, 0.2); color: #22c55e; }
.stat-icon.admin { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }
.stat-icon.deleted { background: rgba(239, 68, 68, 0.2); color: #ef4444; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

/* 탭 */
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.tab svg { width: 18px; height: 18px; }

.tab:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
}

.tab.active {
  background: rgba(168, 85, 247, 0.15);
  border-color: rgba(168, 85, 247, 0.3);
  color: #a855f7;
}

/* 테이블 카드 */
.table-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  overflow: hidden;
}

.table-wrapper { overflow-x: auto; }

.loading, .error-message {
  padding: 60px 20px;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
}

.error-message {
  color: #ef4444;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.error-message svg { width: 40px; height: 40px; }

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #a855f7;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th {
  background: rgba(255, 255, 255, 0.03);
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

.user-table td {
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.user-table tbody tr {
  transition: background 0.2s;
}

.user-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.02);
}

.td-id { color: rgba(255, 255, 255, 0.4); font-size: 13px; }
.td-username { font-weight: 600; color: rgba(255, 255, 255, 0.95); }
.td-email { color: rgba(255, 255, 255, 0.5); font-size: 13px; }
.td-date { color: rgba(255, 255, 255, 0.4); font-size: 13px; white-space: nowrap; }

.role-badge, .status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.role-badge.user { background: rgba(102, 126, 234, 0.2); color: #818cf8; }
.role-badge.admin { background: rgba(239, 68, 68, 0.2); color: #f87171; }
.role-badge.developer { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }

.status-badge.active { background: rgba(34, 197, 94, 0.2); color: #4ade80; }
.status-badge.inactive { background: rgba(255, 255, 255, 0.1); color: rgba(255, 255, 255, 0.4); }

.td-actions {
  display: flex;
  gap: 8px;
}

.btn-action {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.btn-action svg { width: 16px; height: 16px; }

.btn-action.role:hover:not(:disabled) {
  background: rgba(168, 85, 247, 0.3);
  transform: translateY(-2px);
}

.btn-action.danger {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.btn-action.danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.3);
  transform: translateY(-2px);
}

.btn-action.success {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.btn-action.success:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.3);
  transform: translateY(-2px);
}

.btn-action:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal {
  background: rgba(20, 20, 30, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  width: 90%;
  max-width: 480px;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  color: rgba(255, 255, 255, 0.95);
  font-weight: 600;
}

.modal-close {
  background: rgba(255, 255, 255, 0.05);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close svg { width: 18px; height: 18px; }

.modal-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.modal-body { padding: 24px; }

.modal-desc {
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 20px 0;
  font-size: 14px;
}

.modal-desc strong { color: rgba(255, 255, 255, 0.95); }

.role-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-option {
  display: block;
  cursor: pointer;
}

.role-option input { display: none; }

.role-option-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 2px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  transition: all 0.2s;
}

.role-option:hover .role-option-content {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.05);
}

.role-option.selected .role-option-content {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.1);
}

.role-option-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-option-icon svg { width: 22px; height: 22px; }

.role-option-icon.user { background: rgba(102, 126, 234, 0.2); color: #818cf8; }
.role-option-icon.developer { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }
.role-option-icon.admin { background: rgba(239, 68, 68, 0.2); color: #f87171; }

.role-option-title {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
}

.role-option-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

.modal-footer {
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-primary, .btn-secondary {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #a855f7, #6366f1);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(168, 85, 247, 0.4);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

@media (max-width: 768px) {
  .dashboard-header { flex-direction: column; text-align: center; }
  .stats-grid { grid-template-columns: 1fr; }
  .tabs { flex-direction: column; }
  .tab { justify-content: center; }
  .user-table th, .user-table td { padding: 12px 8px; font-size: 12px; }
  .td-actions { flex-direction: column; gap: 4px; }
  .btn-action { width: 32px; height: 32px; }
  .orb-1 { width: 300px; height: 300px; }
  .orb-2 { width: 250px; height: 250px; }
  .orb-3 { width: 180px; height: 180px; }
}
</style>
