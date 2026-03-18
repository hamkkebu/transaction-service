import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import apiClient from '@/api/client';
import { fetchLedgers as fetchLedgersApi, LedgerItem } from '@/api/ledgerApi';
import { API_ENDPOINTS, ROUTES } from '@/constants';
import type { Transaction, TransactionSummary, TransactionType } from '@/types/domain.types';
import type { ApiResponse } from '@/types/api.types';
import { formatCurrency } from '@/utils/currency.utils';
import styles from './TransactionList.module.css';

export default function TransactionList() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [summary, setSummary] = useState<TransactionSummary | null>(null);
  const [ledgers, setLedgers] = useState<LedgerItem[]>([]);
  const [selectedLedgerId, setSelectedLedgerId] = useState(0);
  const [filterType, setFilterType] = useState<'ALL' | TransactionType>('ALL');
  const [selectedMonth, setSelectedMonth] = useState(new Date().toISOString().slice(0, 7));
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [transactionToDelete, setTransactionToDelete] = useState<Transaction | null>(null);

  const filteredTransactions = useMemo(() => {
    if (filterType === 'ALL') {
      return transactions;
    }
    return transactions.filter((tx) => tx.type === filterType);
  }, [transactions, filterType]);

  const formatDate = (dateStr: string): string => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  const fetchLedgers = async () => {
    try {
      const data = await fetchLedgersApi();
      setLedgers(data);
      if (data.length > 0) {
        // 기본 가계부가 있으면 우선 선택, 없으면 첫 번째
        const defaultLedger = data.find((l) => l.isDefault);
        setSelectedLedgerId(defaultLedger ? defaultLedger.ledgerId : data[0].ledgerId);
      }
    } catch (err) {
      console.error('Failed to fetch ledgers:', err);
    }
  };

  const fetchTransactions = async (ledgerId?: number, month?: string) => {
    const lid = ledgerId ?? selectedLedgerId;
    const m = month ?? selectedMonth;

    if (lid === 0) return;

    setLoading(true);
    try {
      const [year, mon] = m.split('-').map(Number);

      // 월별 거래 조회 (query parameter로 전달)
      const response = await apiClient.get<ApiResponse<any>>(
        API_ENDPOINTS.TRANSACTIONS.MONTHLY,
        { params: { ledgerId: lid, year, month: mon } }
      );

      if (response.data.success && response.data.data) {
        setTransactions(response.data.data.transactions || []);
        setSummary({
          ledgerId: lid,
          totalIncome: response.data.data.totalIncome || 0,
          totalExpense: response.data.data.totalExpense || 0,
          balance: response.data.data.balance || 0,
          transactionCount: response.data.data.transactionCount || 0,
        });
      }
    } catch (error) {
      console.error('Failed to fetch transactions:', error);
      setTransactions([]);
      setSummary(null);
    } finally {
      setLoading(false);
    }
  };

  const goToCreate = () => {
    navigate(ROUTES.TRANSACTION_CREATE);
  };

  const goToEdit = (id: number) => {
    navigate(ROUTES.TRANSACTION_EDIT(id));
  };

  const confirmDelete = (tx: Transaction) => {
    setTransactionToDelete(tx);
    setShowDeleteModal(true);
  };

  const closeDeleteModal = () => {
    setShowDeleteModal(false);
    setTransactionToDelete(null);
  };

  const deleteTransaction = async () => {
    if (!transactionToDelete) return;

    try {
      await apiClient.delete(API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionToDelete.id));
      await fetchTransactions();
      closeDeleteModal();
    } catch (error) {
      console.error('Failed to delete transaction:', error);
    }
  };

  const handleLedgerChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const lid = Number(e.target.value);
    setSelectedLedgerId(lid);
    fetchTransactions(lid, selectedMonth);
  };

  const handleFilterTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const ft = e.target.value as 'ALL' | TransactionType;
    setFilterType(ft);
  };

  const handleMonthChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const m = e.target.value;
    setSelectedMonth(m);
    fetchTransactions(selectedLedgerId, m);
  };

  useEffect(() => {
    fetchLedgers();
  }, []);

  useEffect(() => {
    if (selectedLedgerId > 0) {
      fetchTransactions();
    }
  }, []);

  return (
    <div className={styles['transaction-list']}>
      <div className={styles['page-header']}>
        <h1 className={styles['gradient-text']}>거래 내역</h1>
        <p className={styles['subtitle']}>가계부의 수입/지출 내역을 확인하세요</p>
      </div>

      {/* 필터 및 액션 바 */}
      <div className={`${styles['action-bar']} glass`}>
        <div className={styles['filters']}>
          <select
            value={selectedLedgerId}
            onChange={handleLedgerChange}
            className={styles['filter-select']}
          >
            <option value={0}>가계부 선택</option>
            {ledgers.map((ledger) => (
              <option key={ledger.ledgerId} value={ledger.ledgerId}>
                {ledger.name}
              </option>
            ))}
          </select>
          <select
            value={filterType}
            onChange={handleFilterTypeChange}
            className={styles['filter-select']}
          >
            <option value="ALL">전체</option>
            <option value="INCOME">수입</option>
            <option value="EXPENSE">지출</option>
          </select>
          <input
            type="month"
            value={selectedMonth}
            onChange={handleMonthChange}
            className={styles['filter-input']}
          />
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link
            to={`/cards?ledgerId=${selectedLedgerId}`}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.75rem 1.25rem',
              background: 'var(--bg-tertiary)',
              border: '1px solid var(--glass-border)',
              borderRadius: 'var(--radius-md)',
              color: 'var(--text-primary)',
              fontWeight: 600,
              textDecoration: 'none',
              fontSize: '0.9rem',
              transition: 'background 0.2s',
            }}
          >
            &#128179; 카드 연동
          </Link>
          <button className={styles['btn-primary']} onClick={goToCreate}>
            <span className={styles['icon']}>+</span> 거래 추가
          </button>
        </div>
      </div>

      {/* 요약 카드 */}
      {summary && (
        <div className={styles['summary-cards']}>
          <div className={`${styles['summary-card']} ${styles['income']}`}>
            <div className={styles['card-icon']}>
              <span>+</span>
            </div>
            <div className={styles['card-content']}>
              <span className={styles['label']}>총 수입</span>
              <span className={styles['value']}>{formatCurrency(summary.totalIncome)}</span>
            </div>
          </div>
          <div className={`${styles['summary-card']} ${styles['expense']}`}>
            <div className={styles['card-icon']}>
              <span>-</span>
            </div>
            <div className={styles['card-content']}>
              <span className={styles['label']}>총 지출</span>
              <span className={styles['value']}>{formatCurrency(summary.totalExpense)}</span>
            </div>
          </div>
          <div
            className={`${styles['summary-card']} ${styles['balance']} ${
              summary.balance >= 0 ? styles['positive'] : styles['negative']
            }`}
          >
            <div className={styles['card-icon']}>
              <span>=</span>
            </div>
            <div className={styles['card-content']}>
              <span className={styles['label']}>잔액</span>
              <span className={styles['value']}>{formatCurrency(summary.balance)}</span>
            </div>
          </div>
        </div>
      )}

      {/* 로딩 상태 */}
      {loading && (
        <div className={`${styles['loading-state']} glass`}>
          <div className={styles['loading-spinner']}></div>
          <p>거래 내역을 불러오는 중...</p>
        </div>
      )}

      {/* 거래 목록 */}
      {!loading && transactions.length > 0 && (
        <div className={`${styles['transaction-table-wrapper']} glass`}>
          <table className={styles['transaction-table']}>
            <thead>
              <tr>
                <th>날짜</th>
                <th>유형</th>
                <th>카테고리</th>
                <th>내용</th>
                <th className={styles['align-right']}>금액</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {filteredTransactions.map((tx) => (
                <tr key={tx.id} className={styles['transaction-row']}>
                  <td className={styles['date']}>{formatDate(tx.transactionDate)}</td>
                  <td>
                    <span className={`${styles['type-badge']} ${styles[tx.type.toLowerCase()]}`}>
                      {tx.type === 'INCOME' ? '수입' : '지출'}
                    </span>
                  </td>
                  <td className={styles['category']}>{tx.category}</td>
                  <td className={styles['description']}>{tx.description}</td>
                  <td className={`${styles['amount']} ${styles['align-right']} ${styles[tx.type.toLowerCase()]}`}>
                    {tx.type === 'INCOME' ? '+' : '-'}
                    {formatCurrency(tx.amount)}
                  </td>
                  <td className={styles['actions']}>
                    <button
                      className={styles['btn-icon']}
                      onClick={() => goToEdit(tx.id)}
                      title="수정"
                    >
                      <span>&#9998;</span>
                    </button>
                    <button
                      className={`${styles['btn-icon']} ${styles['delete']}`}
                      onClick={() => confirmDelete(tx)}
                      title="삭제"
                    >
                      <span>&times;</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 빈 상태 */}
      {!loading && transactions.length === 0 && (
        <div className={`${styles['empty-state']} glass`}>
          <div className={styles['empty-icon']}>
            <span>&#128200;</span>
          </div>
          <h3>거래 내역이 없습니다</h3>
          <p>첫 번째 거래를 추가해보세요!</p>
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {showDeleteModal && (
        <div className={styles['modal-overlay']} onClick={closeDeleteModal}>
          <div className={`${styles['modal']} glass`} onClick={(e) => e.stopPropagation()}>
            <h3>거래 삭제</h3>
            <p>정말로 이 거래를 삭제하시겠습니까?</p>
            <p className={styles['delete-info']}>
              {transactionToDelete?.description} - {formatCurrency(transactionToDelete?.amount || 0)}
            </p>
            <div className={styles['modal-actions']}>
              <button className={styles['btn-secondary']} onClick={closeDeleteModal}>
                취소
              </button>
              <button className={styles['btn-danger']} onClick={deleteTransaction}>
                삭제
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
