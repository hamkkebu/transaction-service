<template>
  <div class="transaction-form">
    <div class="page-header">
      <button class="btn-back" @click="goBack">
        <span>&larr;</span> 뒤로
      </button>
      <h1 class="gradient-text">{{ isEditMode ? '거래 수정' : '거래 추가' }}</h1>
      <p class="subtitle">{{ isEditMode ? '거래 정보를 수정하세요' : '새로운 거래를 등록하세요' }}</p>
    </div>

    <form @submit.prevent="handleSubmit" class="form-card glass">
      <!-- 가계부 선택 -->
      <div class="form-group">
        <label for="ledgerId">가계부 <span class="required">*</span></label>
        <select id="ledgerId" v-model="form.ledgerId" required :disabled="isEditMode">
          <option :value="0" disabled>가계부를 선택하세요</option>
          <option v-for="ledger in ledgers" :key="ledger.id" :value="ledger.id">
            {{ ledger.name }}
          </option>
        </select>
      </div>

      <!-- 거래 유형 -->
      <div class="form-group">
        <label>거래 유형 <span class="required">*</span></label>
        <div class="type-selector">
          <button
            type="button"
            class="type-btn income"
            :class="{ active: form.type === 'INCOME' }"
            @click="form.type = 'INCOME'"
          >
            <span class="icon">+</span>
            수입
          </button>
          <button
            type="button"
            class="type-btn expense"
            :class="{ active: form.type === 'EXPENSE' }"
            @click="form.type = 'EXPENSE'"
          >
            <span class="icon">-</span>
            지출
          </button>
        </div>
      </div>

      <!-- 금액 -->
      <div class="form-group">
        <label for="amount">금액 <span class="required">*</span></label>
        <div class="amount-input-wrapper">
          <span class="currency-symbol">&#8361;</span>
          <input
            type="number"
            id="amount"
            v-model.number="form.amount"
            placeholder="0"
            min="0"
            step="1"
            required
          />
        </div>
      </div>

      <!-- 카테고리 -->
      <div class="form-group">
        <label for="category">카테고리 <span class="required">*</span></label>
        <select id="category" v-model="form.category" required>
          <option value="" disabled>카테고리를 선택하세요</option>
          <optgroup v-if="form.type === 'INCOME'" label="수입 카테고리">
            <option value="급여">급여</option>
            <option value="부수입">부수입</option>
            <option value="투자수익">투자수익</option>
            <option value="이자">이자</option>
            <option value="용돈">용돈</option>
            <option value="기타수입">기타수입</option>
          </optgroup>
          <optgroup v-else label="지출 카테고리">
            <option value="식비">식비</option>
            <option value="교통비">교통비</option>
            <option value="주거비">주거비</option>
            <option value="통신비">통신비</option>
            <option value="의료비">의료비</option>
            <option value="문화생활">문화생활</option>
            <option value="쇼핑">쇼핑</option>
            <option value="교육">교육</option>
            <option value="경조사">경조사</option>
            <option value="기타지출">기타지출</option>
          </optgroup>
        </select>
      </div>

      <!-- 내용 -->
      <div class="form-group">
        <label for="description">내용 <span class="required">*</span></label>
        <input
          type="text"
          id="description"
          v-model="form.description"
          placeholder="거래 내용을 입력하세요"
          maxlength="100"
          required
        />
      </div>

      <!-- 날짜 -->
      <div class="form-group">
        <label for="transactionDate">날짜 <span class="required">*</span></label>
        <input
          type="date"
          id="transactionDate"
          v-model="form.transactionDate"
          required
        />
      </div>

      <!-- 메모 -->
      <div class="form-group">
        <label for="memo">메모</label>
        <textarea
          id="memo"
          v-model="form.memo"
          placeholder="추가 메모를 입력하세요 (선택)"
          rows="3"
          maxlength="500"
        ></textarea>
      </div>

      <!-- 제출 버튼 -->
      <div class="form-actions">
        <button type="button" class="btn-secondary" @click="goBack">취소</button>
        <button type="submit" class="btn-primary" :disabled="submitting">
          <span v-if="submitting" class="loading-spinner-small"></span>
          {{ isEditMode ? '수정' : '등록' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '@/api/client';
import { API_ENDPOINTS, ROUTES } from '@/constants';
import type { TransactionRequest, TransactionType } from '@/types/domain.types';
import type { ApiResponse } from '@/types/api.types';

interface Ledger {
  id: number;
  name: string;
}

export default defineComponent({
  name: 'TransactionForm',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const submitting = ref(false);
    const ledgers = ref<Ledger[]>([]);

    const isEditMode = computed(() => !!route.params.id);
    const transactionId = computed(() => Number(route.params.id));

    const form = reactive<TransactionRequest>({
      ledgerId: 0,
      type: 'EXPENSE' as TransactionType,
      amount: 0,
      description: '',
      category: '',
      transactionDate: new Date().toISOString().split('T')[0],
      memo: '',
    });

    const fetchLedgers = async () => {
      // TODO: Ledger API 연동 후 구현
      // 임시 데이터
      ledgers.value = [
        { id: 1, name: '기본 가계부' },
      ];
      if (!isEditMode.value && ledgers.value.length > 0) {
        form.ledgerId = ledgers.value[0].id;
      }
    };

    const fetchTransaction = async () => {
      if (!isEditMode.value) return;

      try {
        const response = await apiClient.get<ApiResponse<any>>(
          API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionId.value)
        );

        if (response.data.success && response.data.data) {
          const tx = response.data.data;
          form.ledgerId = tx.ledgerId;
          form.type = tx.type;
          form.amount = tx.amount;
          form.description = tx.description;
          form.category = tx.category;
          form.transactionDate = tx.transactionDate;
          form.memo = tx.memo || '';
        }
      } catch (error) {
        console.error('Failed to fetch transaction:', error);
        alert('거래 정보를 불러오는데 실패했습니다.');
        router.push(ROUTES.TRANSACTIONS);
      }
    };

    const handleSubmit = async () => {
      if (form.ledgerId === 0) {
        alert('가계부를 선택해주세요.');
        return;
      }
      if (form.amount <= 0) {
        alert('금액을 입력해주세요.');
        return;
      }

      submitting.value = true;
      try {
        if (isEditMode.value) {
          await apiClient.put(
            API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionId.value),
            form
          );
          alert('거래가 수정되었습니다.');
        } else {
          await apiClient.post(API_ENDPOINTS.TRANSACTIONS.BASE, form);
          alert('거래가 등록되었습니다.');
        }
        router.push(ROUTES.TRANSACTIONS);
      } catch (error) {
        console.error('Failed to save transaction:', error);
      } finally {
        submitting.value = false;
      }
    };

    const goBack = () => {
      router.push(ROUTES.TRANSACTIONS);
    };

    onMounted(async () => {
      await fetchLedgers();
      if (isEditMode.value) {
        await fetchTransaction();
      }
    });

    return {
      form,
      ledgers,
      isEditMode,
      submitting,
      handleSubmit,
      goBack,
    };
  },
});
</script>

<style scoped>
.transaction-form {
  padding: 2rem;
  max-width: 600px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.page-header {
  margin-bottom: 2rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  margin-bottom: 1rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--hover-bg);
  color: var(--text-primary);
}

.page-header h1 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: var(--text-secondary);
}

.form-card {
  padding: 2rem;
  border-radius: var(--radius-lg);
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--text-primary);
}

.required {
  color: var(--accent-red);
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.875rem 1rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--accent-purple);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: var(--text-tertiary);
}

.type-selector {
  display: flex;
  gap: 1rem;
}

.type-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.type-btn .icon {
  font-size: 1.25rem;
  font-weight: bold;
}

.type-btn.income:hover,
.type-btn.income.active {
  border-color: var(--accent-green);
  background: rgba(16, 185, 129, 0.1);
  color: var(--accent-green);
}

.type-btn.expense:hover,
.type-btn.expense.active {
  border-color: var(--accent-red);
  background: rgba(239, 68, 68, 0.1);
  color: var(--accent-red);
}

.amount-input-wrapper {
  position: relative;
}

.currency-symbol {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
  font-size: 1rem;
}

.amount-input-wrapper input {
  padding-left: 2.5rem;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.25rem;
  font-weight: 600;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-subtle);
}

.btn-secondary {
  padding: 0.875rem 1.5rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: var(--hover-bg);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 2rem;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-md);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .transaction-form {
    padding: 1rem;
  }

  .form-card {
    padding: 1.5rem;
  }

  .type-selector {
    flex-direction: column;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-secondary,
  .btn-primary {
    width: 100%;
    justify-content: center;
  }
}
</style>
