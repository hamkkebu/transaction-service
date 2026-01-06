<template>
  <div class="transaction-list">
    <div class="page-header">
      <h1 class="gradient-text">거래 내역</h1>
      <p class="subtitle">가계부의 수입/지출 내역을 확인하세요</p>
    </div>

    <!-- 필터 및 액션 바 -->
    <div class="action-bar glass">
      <div class="filters">
        <select v-model="selectedLedgerId" class="filter-select" @change="fetchTransactions">
          <option :value="0">가계부 선택</option>
          <option v-for="ledger in ledgers" :key="ledger.id" :value="ledger.id">
            {{ ledger.name }}
          </option>
        </select>
        <select v-model="filterType" class="filter-select" @change="fetchTransactions">
          <option value="ALL">전체</option>
          <option value="INCOME">수입</option>
          <option value="EXPENSE">지출</option>
        </select>
        <input
          type="month"
          v-model="selectedMonth"
          class="filter-input"
          @change="fetchTransactions"
        />
      </div>
      <button class="btn-primary" @click="goToCreate">
        <span class="icon">+</span> 거래 추가
      </button>
    </div>

    <!-- 요약 카드 -->
    <div class="summary-cards" v-if="summary">
      <div class="summary-card income">
        <div class="card-icon">
          <span>+</span>
        </div>
        <div class="card-content">
          <span class="label">총 수입</span>
          <span class="value">{{ formatCurrency(summary.totalIncome) }}</span>
        </div>
      </div>
      <div class="summary-card expense">
        <div class="card-icon">
          <span>-</span>
        </div>
        <div class="card-content">
          <span class="label">총 지출</span>
          <span class="value">{{ formatCurrency(summary.totalExpense) }}</span>
        </div>
      </div>
      <div class="summary-card balance" :class="{ positive: summary.balance >= 0, negative: summary.balance < 0 }">
        <div class="card-icon">
          <span>=</span>
        </div>
        <div class="card-content">
          <span class="label">잔액</span>
          <span class="value">{{ formatCurrency(summary.balance) }}</span>
        </div>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-state glass">
      <div class="loading-spinner"></div>
      <p>거래 내역을 불러오는 중...</p>
    </div>

    <!-- 거래 목록 -->
    <div v-else-if="transactions.length > 0" class="transaction-table-wrapper glass">
      <table class="transaction-table">
        <thead>
          <tr>
            <th>날짜</th>
            <th>유형</th>
            <th>카테고리</th>
            <th>내용</th>
            <th class="align-right">금액</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in filteredTransactions" :key="tx.id" class="transaction-row">
            <td class="date">{{ formatDate(tx.transactionDate) }}</td>
            <td>
              <span class="type-badge" :class="tx.type.toLowerCase()">
                {{ tx.type === 'INCOME' ? '수입' : '지출' }}
              </span>
            </td>
            <td class="category">{{ tx.category }}</td>
            <td class="description">{{ tx.description }}</td>
            <td class="amount align-right" :class="tx.type.toLowerCase()">
              {{ tx.type === 'INCOME' ? '+' : '-' }}{{ formatCurrency(tx.amount) }}
            </td>
            <td class="actions">
              <button class="btn-icon" @click="goToEdit(tx.id)" title="수정">
                <span>&#9998;</span>
              </button>
              <button class="btn-icon delete" @click="confirmDelete(tx)" title="삭제">
                <span>&times;</span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="empty-state glass">
      <div class="empty-icon">
        <span>&#128200;</span>
      </div>
      <h3>거래 내역이 없습니다</h3>
      <p>첫 번째 거래를 추가해보세요!</p>
      <button class="btn-primary" @click="goToCreate">거래 추가</button>
    </div>

    <!-- 삭제 확인 모달 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal glass">
        <h3>거래 삭제</h3>
        <p>정말로 이 거래를 삭제하시겠습니까?</p>
        <p class="delete-info">{{ transactionToDelete?.description }} - {{ formatCurrency(transactionToDelete?.amount || 0) }}</p>
        <div class="modal-actions">
          <button class="btn-secondary" @click="closeDeleteModal">취소</button>
          <button class="btn-danger" @click="deleteTransaction">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '@/api/client';
import { API_ENDPOINTS, ROUTES } from '@/constants';
import type { Transaction, TransactionSummary, TransactionType } from '@/types/domain.types';
import type { ApiResponse } from '@/types/api.types';
import { formatCurrency } from '@/utils/currency.utils';

interface Ledger {
  id: number;
  name: string;
}

export default defineComponent({
  name: 'TransactionList',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const transactions = ref<Transaction[]>([]);
    const summary = ref<TransactionSummary | null>(null);
    const ledgers = ref<Ledger[]>([]);
    const selectedLedgerId = ref(0);
    const filterType = ref<'ALL' | TransactionType>('ALL');
    const selectedMonth = ref(new Date().toISOString().slice(0, 7));
    const showDeleteModal = ref(false);
    const transactionToDelete = ref<Transaction | null>(null);

    const filteredTransactions = computed(() => {
      if (filterType.value === 'ALL') {
        return transactions.value;
      }
      return transactions.value.filter(tx => tx.type === filterType.value);
    });

    const formatDate = (dateStr: string): string => {
      const date = new Date(dateStr);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    };

    const fetchLedgers = async () => {
      // TODO: Ledger API 연동 후 구현
      // 임시 데이터
      ledgers.value = [
        { id: 1, name: '기본 가계부' },
      ];
      if (ledgers.value.length > 0) {
        selectedLedgerId.value = ledgers.value[0].id;
      }
    };

    const fetchTransactions = async () => {
      if (selectedLedgerId.value === 0) return;

      loading.value = true;
      try {
        const [year, month] = selectedMonth.value.split('-').map(Number);

        // 월별 거래 조회
        const response = await apiClient.get<ApiResponse<any>>(
          API_ENDPOINTS.TRANSACTIONS.MONTHLY(selectedLedgerId.value, year, month)
        );

        if (response.data.success && response.data.data) {
          transactions.value = response.data.data.transactions || [];
          summary.value = {
            ledgerId: selectedLedgerId.value,
            totalIncome: response.data.data.totalIncome || 0,
            totalExpense: response.data.data.totalExpense || 0,
            balance: response.data.data.balance || 0,
            transactionCount: response.data.data.transactionCount || 0,
          };
        }
      } catch (error) {
        console.error('Failed to fetch transactions:', error);
        transactions.value = [];
        summary.value = null;
      } finally {
        loading.value = false;
      }
    };

    const goToCreate = () => {
      router.push(ROUTES.TRANSACTION_CREATE);
    };

    const goToEdit = (id: number) => {
      router.push(ROUTES.TRANSACTION_EDIT(id));
    };

    const confirmDelete = (tx: Transaction) => {
      transactionToDelete.value = tx;
      showDeleteModal.value = true;
    };

    const closeDeleteModal = () => {
      showDeleteModal.value = false;
      transactionToDelete.value = null;
    };

    const deleteTransaction = async () => {
      if (!transactionToDelete.value) return;

      try {
        await apiClient.delete(
          API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionToDelete.value.id)
        );
        await fetchTransactions();
        closeDeleteModal();
      } catch (error) {
        console.error('Failed to delete transaction:', error);
      }
    };

    onMounted(async () => {
      await fetchLedgers();
      if (selectedLedgerId.value > 0) {
        await fetchTransactions();
      }
    });

    return {
      loading,
      transactions,
      filteredTransactions,
      summary,
      ledgers,
      selectedLedgerId,
      filterType,
      selectedMonth,
      showDeleteModal,
      transactionToDelete,
      formatCurrency,
      formatDate,
      fetchTransactions,
      goToCreate,
      goToEdit,
      confirmDelete,
      closeDeleteModal,
      deleteTransaction,
    };
  },
});
</script>

<style scoped>
.transaction-list {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: var(--radius-lg);
  margin-bottom: 1.5rem;
}

.filters {
  display: flex;
  gap: 1rem;
}

.filter-select,
.filter-input {
  padding: 0.75rem 1rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 0.9rem;
  cursor: pointer;
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: var(--accent-purple);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-md);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(10px);
}

.summary-card.income .card-icon {
  background: var(--gradient-success);
}

.summary-card.expense .card-icon {
  background: var(--gradient-error);
}

.summary-card.balance .card-icon {
  background: var(--gradient-primary);
}

.card-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
}

.card-content {
  display: flex;
  flex-direction: column;
}

.card-content .label {
  color: var(--text-secondary);
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}

.card-content .value {
  font-size: 1.25rem;
  font-weight: 700;
  font-family: 'Space Grotesk', sans-serif;
}

.summary-card.balance.positive .value {
  color: var(--accent-green);
}

.summary-card.balance.negative .value {
  color: var(--accent-red);
}

.transaction-table-wrapper {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.transaction-table {
  width: 100%;
  border-collapse: collapse;
}

.transaction-table th,
.transaction-table td {
  padding: 1rem 1.25rem;
  text-align: left;
}

.transaction-table th {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.transaction-table tr:not(:last-child) {
  border-bottom: 1px solid var(--border-subtle);
}

.transaction-row:hover {
  background: var(--hover-bg);
}

.type-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
}

.type-badge.income {
  background: rgba(16, 185, 129, 0.2);
  color: var(--accent-green);
}

.type-badge.expense {
  background: rgba(239, 68, 68, 0.2);
  color: var(--accent-red);
}

.amount {
  font-family: 'Space Grotesk', sans-serif;
  font-weight: 600;
}

.amount.income {
  color: var(--accent-green);
}

.amount.expense {
  color: var(--accent-red);
}

.align-right {
  text-align: right;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: var(--hover-bg);
  color: var(--text-primary);
}

.btn-icon.delete:hover {
  background: rgba(239, 68, 68, 0.2);
  color: var(--accent-red);
  border-color: var(--accent-red);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  border-radius: var(--radius-lg);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 1.5rem;
}

/* 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  padding: 2rem;
  border-radius: var(--radius-lg);
  max-width: 400px;
  width: 90%;
}

.modal h3 {
  margin-bottom: 1rem;
}

.delete-info {
  padding: 1rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  margin: 1rem 0;
  color: var(--text-secondary);
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: var(--hover-bg);
}

.btn-danger {
  padding: 0.75rem 1.5rem;
  background: var(--gradient-error);
  border: none;
  border-radius: var(--radius-md);
  color: white;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 768px) {
  .transaction-list {
    padding: 1rem;
  }

  .action-bar {
    flex-direction: column;
    gap: 1rem;
  }

  .filters {
    flex-wrap: wrap;
    width: 100%;
  }

  .filter-select,
  .filter-input {
    flex: 1;
    min-width: 120px;
  }

  .btn-primary {
    width: 100%;
    justify-content: center;
  }

  .summary-cards {
    grid-template-columns: 1fr;
  }

  .transaction-table {
    font-size: 0.85rem;
  }

  .transaction-table th,
  .transaction-table td {
    padding: 0.75rem;
  }
}
</style>
