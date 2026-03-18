import apiClient from './client';
import { API_ENDPOINTS } from '@/constants';
import type { ApiResponse } from '@/types/api.types';
import type {
  CodefAccountRequest,
  CodefCardInfo,
  LinkCardRequest,
  LinkedCardResponse,
  SyncResult,
} from '@/types/domain.types';

export const cardApi = {
  /**
   * 카드사 계정 연결 (Connected ID 발급)
   */
  async connectAccount(request: CodefAccountRequest): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(
      API_ENDPOINTS.CARDS.CONNECT,
      request
    );
    return response.data.data;
  },

  /**
   * 간편인증 2차 확인 요청
   */
  async confirmSimpleAuth(request: { organization: string; twoWayInfo: any }): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(
      API_ENDPOINTS.CARDS.CONNECT_SIMPLE_AUTH,
      request
    );
    return response.data.data;
  },

  /**
   * 기존 Connected ID에 카드사 추가
   */
  async addAccount(connectedId: string, request: CodefAccountRequest): Promise<any> {
    const response = await apiClient.post<ApiResponse<any>>(
      API_ENDPOINTS.CARDS.CONNECT_ADD(connectedId),
      request
    );
    return response.data.data;
  },

  /**
   * Codef 보유카드 목록 조회
   */
  async getCardList(connectedId: string, organization: string): Promise<CodefCardInfo[]> {
    const response = await apiClient.get<ApiResponse<CodefCardInfo[]>>(
      API_ENDPOINTS.CARDS.LIST,
      { params: { connectedId, organization } }
    );
    return response.data.data;
  },

  /**
   * 카드 연동 (가계부에 등록)
   */
  async linkCard(request: LinkCardRequest): Promise<LinkedCardResponse> {
    const response = await apiClient.post<ApiResponse<LinkedCardResponse>>(
      API_ENDPOINTS.CARDS.LINK,
      request
    );
    return response.data.data;
  },

  /**
   * 내 연동 카드 전체 목록 조회
   */
  async getLinkedCards(): Promise<LinkedCardResponse[]> {
    const response = await apiClient.get<ApiResponse<LinkedCardResponse[]>>(
      API_ENDPOINTS.CARDS.BASE
    );
    return response.data.data;
  },

  /**
   * 특정 가계부의 연동 카드 목록
   */
  async getLinkedCardsByLedger(ledgerId: number): Promise<LinkedCardResponse[]> {
    const response = await apiClient.get<ApiResponse<LinkedCardResponse[]>>(
      API_ENDPOINTS.CARDS.BY_LEDGER(ledgerId)
    );
    return response.data.data;
  },

  /**
   * 카드 연동 해제
   */
  async unlinkCard(linkedCardId: number): Promise<void> {
    await apiClient.delete(API_ENDPOINTS.CARDS.UNLINK(linkedCardId));
  },

  /**
   * 카드 승인내역 동기화
   */
  async syncCard(linkedCardId: number): Promise<SyncResult> {
    const response = await apiClient.post<ApiResponse<SyncResult>>(
      API_ENDPOINTS.CARDS.SYNC(linkedCardId)
    );
    return response.data.data;
  },
};

export default cardApi;
