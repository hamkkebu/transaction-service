import apiClient from './client';
import { API_ENDPOINTS } from '@/constants';
import type { ApiResponse } from '@/types/api.types';
import type {
  Transaction,
  TransactionRequest,
  TransactionSummary,
  PeriodTransactionSummary,
} from '@/types/domain.types';

/**
 * 페이징 응답 타입
 */
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

export const transactionApi = {
  /**
   * 거래 목록 조회 (페이징)
   */
  async getTransactions(ledgerId: number, page = 0, size = 20): Promise<PageResponse<Transaction>> {
    const response = await apiClient.get<ApiResponse<PageResponse<Transaction>>>(
      API_ENDPOINTS.TRANSACTIONS.BASE,
      { params: { ledgerId, page, size } }
    );
    return response.data.data;
  },

  /**
   * 거래 전체 목록 조회
   */
  async getAllTransactions(ledgerId: number): Promise<Transaction[]> {
    const response = await apiClient.get<ApiResponse<Transaction[]>>(
      API_ENDPOINTS.TRANSACTIONS.ALL,
      { params: { ledgerId } }
    );
    return response.data.data;
  },

  /**
   * 거래 상세 조회
   */
  async getTransaction(id: number): Promise<Transaction> {
    const response = await apiClient.get<ApiResponse<Transaction>>(
      API_ENDPOINTS.TRANSACTIONS.BY_ID(id)
    );
    return response.data.data;
  },

  /**
   * 거래 요약 조회
   */
  async getSummary(ledgerId: number): Promise<TransactionSummary> {
    const response = await apiClient.get<ApiResponse<TransactionSummary>>(
      API_ENDPOINTS.TRANSACTIONS.SUMMARY,
      { params: { ledgerId } }
    );
    return response.data.data;
  },

  /**
   * 거래 생성
   */
  async createTransaction(request: TransactionRequest): Promise<Transaction> {
    const response = await apiClient.post<ApiResponse<Transaction>>(
      API_ENDPOINTS.TRANSACTIONS.BASE,
      request
    );
    return response.data.data;
  },

  /**
   * 거래 수정
   */
  async updateTransaction(id: number, request: TransactionRequest): Promise<Transaction> {
    const response = await apiClient.put<ApiResponse<Transaction>>(
      API_ENDPOINTS.TRANSACTIONS.BY_ID(id),
      request
    );
    return response.data.data;
  },

  /**
   * 거래 삭제
   */
  async deleteTransaction(id: number): Promise<void> {
    await apiClient.delete(API_ENDPOINTS.TRANSACTIONS.BY_ID(id));
  },

  // ==================== 기간별 조회 API ====================

  /**
   * 일별 거래 요약 조회
   */
  async getDailySummary(ledgerId: number, date: string): Promise<PeriodTransactionSummary> {
    const response = await apiClient.get<ApiResponse<PeriodTransactionSummary>>(
      API_ENDPOINTS.TRANSACTIONS.DAILY,
      { params: { ledgerId, date } }
    );
    return response.data.data;
  },

  /**
   * 월별 거래 요약 조회
   */
  async getMonthlySummary(ledgerId: number, year: number, month: number): Promise<PeriodTransactionSummary> {
    const response = await apiClient.get<ApiResponse<PeriodTransactionSummary>>(
      API_ENDPOINTS.TRANSACTIONS.MONTHLY,
      { params: { ledgerId, year, month } }
    );
    return response.data.data;
  },

  /**
   * 년별 거래 요약 조회
   */
  async getYearlySummary(ledgerId: number, year: number): Promise<PeriodTransactionSummary> {
    const response = await apiClient.get<ApiResponse<PeriodTransactionSummary>>(
      API_ENDPOINTS.TRANSACTIONS.YEARLY,
      { params: { ledgerId, year } }
    );
    return response.data.data;
  },

  /**
   * 기간별 거래 요약 조회
   */
  async getPeriodSummary(ledgerId: number, startDate: string, endDate: string): Promise<PeriodTransactionSummary> {
    const response = await apiClient.get<ApiResponse<PeriodTransactionSummary>>(
      API_ENDPOINTS.TRANSACTIONS.PERIOD,
      { params: { ledgerId, startDate, endDate } }
    );
    return response.data.data;
  },
};

export default transactionApi;
