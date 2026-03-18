import apiClient from './client';
import { API_ENDPOINTS } from '@/constants';

export interface LedgerItem {
  ledgerId: number;
  name: string;
  description: string;
  currency: string;
  isDefault: boolean;
}

/**
 * 가계부 목록 조회 (Kafka 동기화 데이터)
 */
export async function fetchLedgers(): Promise<LedgerItem[]> {
  const response = await apiClient.get(API_ENDPOINTS.LEDGERS);
  return response.data.data || [];
}

export default { fetchLedgers };
