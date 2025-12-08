<template>
  <div class="leave-container">
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="leave-card">
      <div class="warning-icon">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
      </div>

      <div class="leave-header">
        <h1 class="leave-title">회원탈퇴</h1>
        <p class="leave-subtitle">정말로 탈퇴하시겠습니까?</p>
      </div>

      <div class="warning-message">
        <div class="warning-content">
          <h3>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            탈퇴 시 유의사항
          </h3>
          <ul>
            <li>계정의 모든 정보가 영구적으로 삭제됩니다</li>
            <li>삭제된 데이터는 복구할 수 없습니다</li>
            <li>탈퇴 후에도 동일한 아이디 및 닉네임으로 재가입할 수 없습니다</li>
          </ul>
        </div>
      </div>

      <form class="leave-form" @submit.prevent="confirmAndLeave">
        <div class="input-group">
          <label for="password" class="input-label">비밀번호 확인</label>
          <div class="input-wrapper">
            <div class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
            </div>
            <input
              id="password"
              type="password"
              class="input-field"
              placeholder="비밀번호를 입력하세요"
              v-model="password"
              required
            />
          </div>
        </div>

        <div class="confirmation-checkbox">
          <label class="checkbox-label" @click.prevent="confirmed = !confirmed">
            <div class="custom-checkbox" :class="{ checked: confirmed }">
              <svg v-if="confirmed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </div>
            <span>위 유의사항을 모두 확인했으며, 탈퇴에 동의합니다</span>
          </label>
        </div>

        <div class="button-group">
          <button type="button" @click="goBack" class="btn-cancel">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
            <span>취소</span>
          </button>
          <button type="submit" class="btn-leave" :disabled="!confirmed || !password || loading">
            <span v-if="!loading">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              <span>탈퇴하기</span>
            </span>
            <span v-else class="loading-spinner"></span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '@/api/client';
import { useApi } from '@/composables/useApi';
import { useAuth } from '@/composables/useAuth';
import { API_ENDPOINTS, ROUTES, SUCCESS_MESSAGES } from '@/constants';

export default defineComponent({
  name: 'LeaveUser',
  setup() {
    const router = useRouter();
    const { loading, execute } = useApi();
    const { currentUser, logout, isAuthenticated } = useAuth();

    const password = ref('');
    const confirmed = ref(false);

    onMounted(() => {
      // Keycloak 인증은 App.vue에서 initAuth로 처리됨
      if (!isAuthenticated.value) {
        alert('로그인이 필요합니다.');
        router.push(ROUTES.LOGIN);
      }
    });

    const confirmAndLeave = () => {
      if (!confirmed.value) {
        alert('탈퇴 동의에 체크해주세요.');
        return;
      }

      if (!password.value) {
        alert('비밀번호를 입력해주세요.');
        return;
      }

      if (!currentUser.value) {
        alert('로그인 정보를 찾을 수 없습니다.');
        router.push(ROUTES.LOGIN);
        return;
      }

      if (
        confirm(
          `정말로 "${currentUser.value.username}" 계정을 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`
        )
      ) {
        leaveSubmit();
      }
    };

    const leaveSubmit = async () => {
      if (!currentUser.value) {
        return;
      }

      await execute(
        () =>
          apiClient.delete(API_ENDPOINTS.USER_BY_USERNAME(currentUser.value!.username), {
            data: {
              password: password.value,
            },
            headers: {
              'Refresh-Token': localStorage.getItem('refreshToken') || '',
            },
          }),
        {
          onSuccess: async () => {
            alert('회원 탈퇴가 완료되었습니다.');
            await logout();
            router.push(ROUTES.LOGIN);
          },
        }
      );
    };

    const goBack = () => {
      router.push(ROUTES.USER_INFO);
    };

    return {
      password,
      confirmed,
      loading,
      confirmAndLeave,
      goBack,
    };
  },
});
</script>

<style scoped>
.leave-container {
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

.orb-1 { width: 500px; height: 500px; background: linear-gradient(135deg, #ef4444, #dc2626); top: -150px; right: -150px; }
.orb-2 { width: 400px; height: 400px; background: linear-gradient(135deg, #f97316, #ea580c); bottom: -100px; left: -100px; animation-delay: -7s; }
.orb-3 { width: 300px; height: 300px; background: linear-gradient(135deg, #667eea, #764ba2); top: 40%; left: 30%; animation-delay: -14s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 30px) scale(0.95); }
}

.leave-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 40px;
  width: 100%;
  max-width: 480px;
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.warning-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
  box-shadow: 0 8px 32px rgba(239, 68, 68, 0.4);
}

@keyframes pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 8px 32px rgba(239, 68, 68, 0.4); }
  50% { transform: scale(1.05); box-shadow: 0 12px 40px rgba(239, 68, 68, 0.5); }
}

.warning-icon svg { width: 36px; height: 36px; color: white; }

.leave-header { text-align: center; margin-bottom: 24px; }

.leave-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 28px; font-weight: 700;
  background: linear-gradient(135deg, #fff, rgba(255,255,255,0.7));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; margin: 0 0 8px 0;
}

.leave-subtitle { color: rgba(255, 255, 255, 0.5); font-size: 14px; margin: 0; }

.warning-message {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.warning-content h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ef4444;
  font-size: 15px;
  margin: 0 0 12px 0;
  font-weight: 600;
}

.warning-content h3 svg { width: 18px; height: 18px; }

.warning-content ul {
  margin: 0;
  padding-left: 20px;
  color: rgba(255, 255, 255, 0.7);
}

.warning-content li {
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.leave-form { display: flex; flex-direction: column; gap: 20px; }

.input-group { display: flex; flex-direction: column; gap: 8px; }

.input-label { font-size: 13px; font-weight: 500; color: rgba(255, 255, 255, 0.7); }

.input-wrapper { position: relative; display: flex; align-items: center; }

.input-icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.3);
  pointer-events: none;
  transition: color 0.3s;
  z-index: 2;
}

.input-icon svg { width: 18px; height: 18px; }

.input-field {
  width: 100%;
  padding: 14px 14px 14px 44px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  transition: all 0.3s ease;
  outline: none;
}

.input-field::placeholder { color: rgba(255, 255, 255, 0.25); }

.input-field:hover { border-color: rgba(255, 255, 255, 0.15); background: rgba(255, 255, 255, 0.05); }

.input-field:focus {
  border-color: rgba(239, 68, 68, 0.5);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.input-wrapper:focus-within .input-icon { color: #ef4444; }

.confirmation-checkbox {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s;
}

.confirmation-checkbox:hover {
  border-color: rgba(255, 255, 255, 0.15);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.custom-checkbox {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.custom-checkbox.checked {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border-color: #ef4444;
}

.custom-checkbox svg { width: 12px; height: 12px; color: white; }

.button-group { display: flex; gap: 12px; margin-top: 8px; }

.btn-cancel,
.btn-leave {
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

.btn-cancel svg,
.btn-leave svg { width: 18px; height: 18px; }

.btn-cancel {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.btn-leave {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.4);
}

.btn-leave:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(239, 68, 68, 0.5);
}

.btn-leave:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .leave-card { padding: 28px 20px; }
  .leave-title { font-size: 24px; }
  .warning-icon { width: 64px; height: 64px; }
  .warning-icon svg { width: 32px; height: 32px; }
  .button-group { flex-direction: column; }
  .orb-1 { width: 300px; height: 300px; }
  .orb-2 { width: 250px; height: 250px; }
  .orb-3 { width: 180px; height: 180px; }
}
</style>
