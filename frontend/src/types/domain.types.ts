/**
 * 도메인 타입 정의
 */

/**
 * 거래 유형
 */
export type TransactionType = 'INCOME' | 'EXPENSE';

/**
 * 거래 엔티티
 */
export interface Transaction {
  id: number;
  ledgerId: number;
  userId: number;
  type: TransactionType;
  amount: number;
  description: string;
  category: string;
  transactionDate: string;
  memo?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 거래 생성 요청 DTO
 */
export interface TransactionRequest {
  ledgerId: number;
  type: TransactionType;
  amount: number;
  description: string;
  category: string;
  transactionDate: string;
  memo?: string;
}

/**
 * 거래 요약 정보
 */
export interface TransactionSummary {
  ledgerId: number;
  totalIncome: number;
  totalExpense: number;
  balance: number;
  transactionCount: number;
}

/**
 * 기간별 거래 요약
 */
export interface PeriodTransactionSummary {
  ledgerId: number;
  periodType: 'DAILY' | 'MONTHLY' | 'YEARLY';
  startDate: string;
  endDate: string;
  totalIncome: number;
  totalExpense: number;
  balance: number;
  transactionCount: number;
  transactions: Transaction[];
  periodDetails?: PeriodDetail[];
}

/**
 * 기간 상세 정보
 */
export interface PeriodDetail {
  periodLabel: string;
  startDate: string;
  endDate: string;
  income: number;
  expense: number;
  balance: number;
  transactionCount: number;
}

/**
 * 카테고리 엔티티
 */
export interface Category {
  categoryId: number;
  ledgerId: number;
  name: string;
  color: string;
  createdAt: string;
  updatedAt: string;
}

// ==================== Codef 카드 연동 관련 ====================

/**
 * 카드사 계정 등록 요청
 */
export interface CodefAccountRequest {
  organization: string;
  loginType?: string;        // "1" = ID/PW (기본), "5" = 간편인증
  loginTypeLevel?: string;   // 간편인증 수단 (1~8)
  loginId?: string;
  loginPw?: string;
}

/**
 * Codef에서 조회된 카드 정보
 */
export interface CodefCardInfo {
  cardId: string;
  cardName: string;
  cardNoMasked: string;
  organization: string;
  organizationName: string;
}

/**
 * 카드 연동 요청
 */
export interface LinkCardRequest {
  ledgerId: number;
  connectedId: string;
  organization: string;
  cardId: string;
  cardName: string;
  cardNoMasked?: string;
}

/**
 * 연동된 카드 응답
 */
export interface LinkedCardResponse {
  linkedCardId: number;
  ledgerId: number;
  organization: string;
  organizationName: string;
  cardId: string;
  cardName: string;
  cardNoMasked: string;
  lastSyncedDate: string | null;
  createdAt: string;
}

/**
 * 동기화 결과
 */
export interface SyncResult {
  insertedCount: number;
  updatedCount: number;
  skippedCount: number;
  syncPeriod: string;
}

/**
 * 카드사 기관 코드 맵핑
 */
export const CARD_ORGANIZATIONS: Record<string, string> = {
  '0301': 'KB국민카드',
  '0302': '현대카드',
  '0303': '삼성카드',
  '0304': 'NH농협카드',
  '0305': 'BC카드',
  '0306': '신한카드',
  '0307': '씨티카드',
  '0309': '우리카드',
  '0310': '하나카드',
  '0311': '롯데카드',
  '0312': '전북카드',
  '0313': '광주카드',
  '0314': '수협카드',
  '0315': '제주카드',
};
