import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import apiClient from '@/api/client';
import { fetchLedgers as fetchLedgersApi, LedgerItem } from '@/api/ledgerApi';
import { API_ENDPOINTS, ROUTES } from '@/constants';
import type { TransactionRequest, TransactionType } from '@/types/domain.types';
import type { ApiResponse } from '@/types/api.types';
import styles from './TransactionForm.module.css';

export default function TransactionForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [submitting, setSubmitting] = useState(false);
  const [ledgers, setLedgers] = useState<LedgerItem[]>([]);
  const [form, setForm] = useState<TransactionRequest>({
    ledgerId: 0,
    type: 'EXPENSE' as TransactionType,
    amount: 0,
    description: '',
    category: '',
    transactionDate: new Date().toISOString().split('T')[0],
    memo: '',
  });

  const isEditMode = useMemo(() => !!id, [id]);
  const transactionId = useMemo(() => (id ? Number(id) : null), [id]);

  const fetchLedgers = async () => {
    try {
      const data = await fetchLedgersApi();
      setLedgers(data);
      if (!isEditMode && data.length > 0) {
        const defaultLedger = data.find((l) => l.isDefault);
        setForm((prev) => ({
          ...prev,
          ledgerId: defaultLedger ? defaultLedger.ledgerId : data[0].ledgerId,
        }));
      }
    } catch (err) {
      console.error('Failed to fetch ledgers:', err);
    }
  };

  const fetchTransaction = async () => {
    if (!isEditMode || !transactionId) return;

    try {
      const response = await apiClient.get<ApiResponse<any>>(
        API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionId)
      );

      if (response.data.success && response.data.data) {
        const tx = response.data.data;
        setForm({
          ledgerId: tx.ledgerId,
          type: tx.type,
          amount: tx.amount,
          description: tx.description,
          category: tx.category,
          transactionDate: tx.transactionDate,
          memo: tx.memo || '',
        });
      }
    } catch (error) {
      console.error('Failed to fetch transaction:', error);
      alert('거래 정보를 불러오는데 실패했습니다.');
      navigate(ROUTES.TRANSACTIONS);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (form.ledgerId === 0) {
      alert('가계부를 선택해주세요.');
      return;
    }
    if (form.amount <= 0) {
      alert('금액을 입력해주세요.');
      return;
    }

    setSubmitting(true);
    try {
      if (isEditMode && transactionId) {
        await apiClient.put(API_ENDPOINTS.TRANSACTIONS.BY_ID(transactionId), form);
        alert('거래가 수정되었습니다.');
      } else {
        await apiClient.post(API_ENDPOINTS.TRANSACTIONS.BASE, form);
        alert('거래가 등록되었습니다.');
      }
      navigate(ROUTES.TRANSACTIONS);
    } catch (error) {
      console.error('Failed to save transaction:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const goBack = () => {
    navigate(ROUTES.TRANSACTIONS);
  };

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    if (name === 'amount') {
      setForm((prev) => ({ ...prev, [name]: Number(value) || 0 }));
    } else {
      setForm((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleTypeChange = (type: TransactionType) => {
    setForm((prev) => ({ ...prev, type, category: '' }));
  };

  useEffect(() => {
    fetchLedgers();
  }, []);

  useEffect(() => {
    if (isEditMode) {
      fetchTransaction();
    }
  }, [isEditMode]);

  const incomeCategories = [
    '급여',
    '부수입',
    '투자수익',
    '이자',
    '용돈',
    '기타수입',
  ];

  const expenseCategories = [
    '식비',
    '교통비',
    '주거비',
    '통신비',
    '의료비',
    '문화생활',
    '쇼핑',
    '교육',
    '경조사',
    '기타지출',
  ];

  const categories = form.type === 'INCOME' ? incomeCategories : expenseCategories;

  return (
    <div className={styles['transaction-form']}>
      <div className={styles['page-header']}>
        <button
          type="button"
          className={styles['btn-back']}
          onClick={goBack}
        >
          <span>&larr;</span> 뒤로
        </button>
        <h1 className={styles['gradient-text']}>
          {isEditMode ? '거래 수정' : '거래 추가'}
        </h1>
        <p className={styles['subtitle']}>
          {isEditMode ? '거래 정보를 수정하세요' : '새로운 거래를 등록하세요'}
        </p>
      </div>

      <form onSubmit={handleSubmit} className={`${styles['form-card']} glass`}>
        {/* 가계부 선택 */}
        <div className={styles['form-group']}>
          <label htmlFor="ledgerId">
            가계부 <span className={styles['required']}>*</span>
          </label>
          <select
            id="ledgerId"
            name="ledgerId"
            value={form.ledgerId}
            onChange={handleInputChange}
            required
            disabled={isEditMode}
          >
            <option value={0}>가계부를 선택하세요</option>
            {ledgers.map((ledger) => (
              <option key={ledger.ledgerId} value={ledger.ledgerId}>
                {ledger.name}
              </option>
            ))}
          </select>
        </div>

        {/* 거래 유형 */}
        <div className={styles['form-group']}>
          <label>
            거래 유형 <span className={styles['required']}>*</span>
          </label>
          <div className={styles['type-selector']}>
            <button
              type="button"
              className={`${styles['type-btn']} ${styles['income']} ${
                form.type === 'INCOME' ? styles['active'] : ''
              }`}
              onClick={() => handleTypeChange('INCOME')}
            >
              <span className={styles['icon']}>+</span>
              수입
            </button>
            <button
              type="button"
              className={`${styles['type-btn']} ${styles['expense']} ${
                form.type === 'EXPENSE' ? styles['active'] : ''
              }`}
              onClick={() => handleTypeChange('EXPENSE')}
            >
              <span className={styles['icon']}>-</span>
              지출
            </button>
          </div>
        </div>

        {/* 금액 */}
        <div className={styles['form-group']}>
          <label htmlFor="amount">
            금액 <span className={styles['required']}>*</span>
          </label>
          <div className={styles['amount-input-wrapper']}>
            <span className={styles['currency-symbol']}>&#8361;</span>
            <input
              type="number"
              id="amount"
              name="amount"
              value={form.amount}
              onChange={handleInputChange}
              placeholder="0"
              min="0"
              step="1"
              required
            />
          </div>
        </div>

        {/* 카테고리 */}
        <div className={styles['form-group']}>
          <label htmlFor="category">
            카테고리 <span className={styles['required']}>*</span>
          </label>
          <select
            id="category"
            name="category"
            value={form.category}
            onChange={handleInputChange}
            required
          >
            <option value="">카테고리를 선택하세요</option>
            {form.type === 'INCOME' && (
              <optgroup label="수입 카테고리">
                {incomeCategories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </optgroup>
            )}
            {form.type === 'EXPENSE' && (
              <optgroup label="지출 카테고리">
                {expenseCategories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </optgroup>
            )}
          </select>
        </div>

        {/* 내용 */}
        <div className={styles['form-group']}>
          <label htmlFor="description">
            내용 <span className={styles['required']}>*</span>
          </label>
          <input
            type="text"
            id="description"
            name="description"
            value={form.description}
            onChange={handleInputChange}
            placeholder="거래 내용을 입력하세요"
            maxLength={100}
            required
          />
        </div>

        {/* 날짜 */}
        <div className={styles['form-group']}>
          <label htmlFor="transactionDate">
            날짜 <span className={styles['required']}>*</span>
          </label>
          <input
            type="date"
            id="transactionDate"
            name="transactionDate"
            value={form.transactionDate}
            onChange={handleInputChange}
            required
          />
        </div>

        {/* 메모 */}
        <div className={styles['form-group']}>
          <label htmlFor="memo">메모</label>
          <textarea
            id="memo"
            name="memo"
            value={form.memo}
            onChange={handleInputChange}
            placeholder="추가 메모를 입력하세요 (선택)"
            rows={3}
            maxLength={500}
          ></textarea>
        </div>

        {/* 제출 버튼 */}
        <div className={styles['form-actions']}>
          <button
            type="button"
            className={styles['btn-secondary']}
            onClick={goBack}
          >
            취소
          </button>
          <button
            type="submit"
            className={styles['btn-primary']}
            disabled={submitting}
          >
            {submitting && <span className={styles['loading-spinner-small']}></span>}
            {isEditMode ? '수정' : '등록'}
          </button>
        </div>
      </form>
    </div>
  );
}
