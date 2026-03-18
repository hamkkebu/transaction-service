import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { cardApi } from '@/api/cardApi';
import { fetchLedgers as fetchLedgersApi, LedgerItem } from '@/api/ledgerApi';
import type {
  CodefCardInfo,
  LinkedCardResponse,
  SyncResult,
  CARD_ORGANIZATIONS,
} from '@/types/domain.types';
import { CARD_ORGANIZATIONS as ORG_MAP } from '@/types/domain.types';
import styles from './LinkedCards.module.css';

type Step = 'list' | 'connect' | 'select' | 'waiting';

type LoginType = 'idpw' | 'simple';

const SIMPLE_AUTH_METHODS = [
  { value: '1', label: '카카오톡', icon: '💬' },
  { value: '2', label: '페이코', icon: '💳' },
  { value: '3', label: '삼성패스', icon: '📱' },
  { value: '4', label: 'KB모바일', icon: '🏦' },
  { value: '5', label: '통신사 PASS', icon: '📶' },
  { value: '6', label: '네이버', icon: '🟢' },
  { value: '7', label: '신한인증서', icon: '🔐' },
  { value: '8', label: '토스', icon: '🔵' },
];

export default function LinkedCards() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  // 상태
  const [step, setStep] = useState<Step>('list');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [linkedCards, setLinkedCards] = useState<LinkedCardResponse[]>([]);
  const [allLinkedCards, setAllLinkedCards] = useState<LinkedCardResponse[]>([]);
  const [ledgers, setLedgers] = useState<LedgerItem[]>([]);
  const [filterLedgerId, setFilterLedgerId] = useState<number>(0);

  // 카드사 연결 폼
  const [loginType, setLoginType] = useState<LoginType>('idpw');
  const [organization, setOrganization] = useState('');
  const [loginId, setLoginId] = useState('');
  const [loginPw, setLoginPw] = useState('');
  const [loginTypeLevel, setLoginTypeLevel] = useState('');
  const [connecting, setConnecting] = useState(false);

  // 간편인증 2-way
  const [twoWayInfo, setTwoWayInfo] = useState<any>(null);
  const [confirming, setConfirming] = useState(false);

  // 카드 선택
  const [connectedId, setConnectedId] = useState('');
  const [availableCards, setAvailableCards] = useState<CodefCardInfo[]>([]);
  const [selectedCardIds, setSelectedCardIds] = useState<Set<string>>(new Set());
  const [selectedLedgerIds, setSelectedLedgerIds] = useState<Set<number>>(new Set());
  const [linking, setLinking] = useState(false);

  // 동기화
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [syncResult, setSyncResult] = useState<SyncResult | null>(null);

  // 삭제 모달
  const [showUnlinkModal, setShowUnlinkModal] = useState(false);
  const [cardToUnlink, setCardToUnlink] = useState<LinkedCardResponse | null>(null);

  // 초기 데이터 로드
  useEffect(() => {
    loadLedgers();
    loadAllLinkedCards();
  }, []);

  // URL에서 ledgerId 읽어서 초기 필터 설정
  useEffect(() => {
    const ledgerIdParam = searchParams.get('ledgerId');
    if (ledgerIdParam) {
      setFilterLedgerId(Number(ledgerIdParam));
    }
  }, [searchParams]);

  // 필터 변경 시 카드 목록 갱신
  useEffect(() => {
    if (filterLedgerId > 0) {
      setLinkedCards(allLinkedCards.filter((c) => Number(c.ledgerId) === Number(filterLedgerId)));
    } else {
      setLinkedCards(allLinkedCards);
    }
  }, [filterLedgerId, allLinkedCards]);

  // 가계부 선택 변경 시 이미 연동된 카드 pre-check
  useEffect(() => {
    if (step !== 'select' || availableCards.length === 0 || selectedLedgerIds.size === 0) return;
    const preChecked = new Set<string>();
    for (const card of availableCards) {
      const linked = allLinkedCards.some(
        (lc) =>
          lc.cardId === card.cardId &&
          lc.organization === card.organization &&
          selectedLedgerIds.has(lc.ledgerId)
      );
      if (linked) {
        preChecked.add(card.cardId);
      }
    }
    // 기존 선택과 합치기 (사용자가 수동으로 선택한 것도 유지)
    setSelectedCardIds((prev) => {
      const next = new Set(prev);
      preChecked.forEach((id) => next.add(id));
      return next;
    });
  }, [selectedLedgerIds, step, availableCards]);

  // 동기화 결과 토스트 자동 닫기
  useEffect(() => {
    if (syncResult) {
      const timer = setTimeout(() => setSyncResult(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [syncResult]);

  const loadAllLinkedCards = async () => {
    setLoading(true);
    try {
      const cards = await cardApi.getLinkedCards();
      setAllLinkedCards(cards);
    } catch (err: any) {
      console.error('Failed to load linked cards:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadLedgers = async () => {
    try {
      const data = await fetchLedgersApi();
      setLedgers(data);
      if (data.length > 0) {
        const defaultLedger = data.find((l) => l.isDefault);
        const defaultId = defaultLedger ? defaultLedger.ledgerId : data[0].ledgerId;
        setSelectedLedgerIds(new Set([defaultId]));
      }
    } catch (err) {
      console.error('Failed to load ledgers:', err);
    }
  };

  // Step 1: 카드사 계정 연결
  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setConnecting(true);

    try {
      if (loginType === 'simple') {
        // 간편인증: 1차 요청 (앱 인증 요청 발송)
        const result = await cardApi.connectAccount({
          organization,
          loginType: '5',
          loginTypeLevel,
        } as any);

        // Codef 응답 구조: { result: {code, message}, data: {...} }
        // CF-03002 = 추가인증 대기 → twoWayInfo 저장 후 대기 화면
        const resultCode = result?.result?.code;
        if (resultCode === 'CF-03002') {
          // twoWayInfo: data 내부 또는 data 자체에 jobIndex 등 포함
          const twInfo = result?.data?.twoWayInfo || result?.data;
          setTwoWayInfo(twInfo);
          setStep('waiting');
        } else if (resultCode === 'CF-00000') {
          // 바로 성공한 경우
          const cid = result?.data?.connectedId || result?.connectedId;
          if (cid) {
            setConnectedId(cid);
            const cards = await cardApi.getCardList(cid, organization);
            setAvailableCards(cards);
            setStep('select');
          }
        } else {
          // 기타 에러
          const msg = result?.result?.message || '간편인증 요청에 실패했습니다.';
          setError(msg);
        }
      } else {
        // ID/PW 방식
        const result = await cardApi.connectAccount({
          organization,
          loginId,
          loginPw,
        } as any);

        const cid = result?.data?.connectedId || result?.connectedId;
        if (!cid) {
          setError('Connected ID를 받지 못했습니다. 응답을 확인해주세요.');
          setConnecting(false);
          return;
        }

        setConnectedId(cid);

        // 보유카드 조회
        const cards = await cardApi.getCardList(cid, organization);
        setAvailableCards(cards);
        setStep('select');
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || err.message || '카드사 연결에 실패했습니다.';
      setError(msg);
    } finally {
      setConnecting(false);
    }
  };

  // 간편인증 2차 확인
  const handleConfirmSimpleAuth = async () => {
    setError('');
    setConfirming(true);

    try {
      const result = await cardApi.confirmSimpleAuth({
        organization,
        twoWayInfo,
      });

      // Codef 응답: { result: {code, message}, data: {connectedId, ...} }
      const resultCode = result?.result?.code;
      if (resultCode && resultCode !== 'CF-00000') {
        setError(result?.result?.message || '간편인증 확인에 실패했습니다.');
        setConfirming(false);
        return;
      }

      const cid = result?.data?.connectedId || result?.connectedId;
      if (!cid) {
        setError('Connected ID를 받지 못했습니다.');
        setConfirming(false);
        return;
      }

      setConnectedId(cid);

      const cards = await cardApi.getCardList(cid, organization);
      setAvailableCards(cards);
      setStep('select');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err.message || '간편인증 확인에 실패했습니다.';
      setError(msg);
    } finally {
      setConfirming(false);
    }
  };

  // Step 2: 카드 선택 토글
  const toggleCardSelection = (cardId: string) => {
    setSelectedCardIds((prev) => {
      const next = new Set(prev);
      if (next.has(cardId)) {
        next.delete(cardId);
      } else {
        next.add(cardId);
      }
      return next;
    });
  };

  // 전체 선택/해제
  const toggleSelectAll = () => {
    if (selectedCardIds.size === availableCards.length) {
      setSelectedCardIds(new Set());
    } else {
      setSelectedCardIds(new Set(availableCards.map((c) => c.cardId)));
    }
  };

  // 이미 연동된 카드인지 확인 (선택된 가계부 기준)
  const isAlreadyLinked = (card: CodefCardInfo): boolean => {
    return allLinkedCards.some(
      (lc) =>
        lc.cardId === card.cardId &&
        lc.organization === card.organization &&
        selectedLedgerIds.has(lc.ledgerId)
    );
  };

  // Step 2: 선택한 카드들 연동
  const handleLinkCards = async () => {
    if (selectedCardIds.size === 0) {
      setError('연동할 카드를 선택해주세요.');
      return;
    }
    if (selectedLedgerIds.size === 0) {
      setError('가계부를 선택해주세요.');
      return;
    }

    setError('');
    setLinking(true);

    try {
      for (const ledgerId of selectedLedgerIds) {
        for (const cardId of selectedCardIds) {
          const card = availableCards.find((c) => c.cardId === cardId);
          if (!card) continue;

          await cardApi.linkCard({
            ledgerId,
            connectedId,
            organization,
            cardId: card.cardId,
            cardName: card.cardName,
            cardNoMasked: card.cardNoMasked,
          });
        }
      }

      // 완료 후 목록으로
      await loadAllLinkedCards();
      resetForm();
      setStep('list');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err.message || '카드 연동에 실패했습니다.';
      setError(msg);
    } finally {
      setLinking(false);
    }
  };

  // 동기화
  const handleSync = async (linkedCardId: number) => {
    setSyncingId(linkedCardId);
    try {
      const result = await cardApi.syncCard(linkedCardId);
      setSyncResult(result);
      await loadAllLinkedCards(); // lastSyncedDate 갱신
    } catch (err: any) {
      const msg = err?.response?.data?.message || err.message || '동기화에 실패했습니다.';
      setError(msg);
    } finally {
      setSyncingId(null);
    }
  };

  // 연동 해제
  const confirmUnlink = (card: LinkedCardResponse) => {
    setCardToUnlink(card);
    setShowUnlinkModal(true);
  };

  const handleUnlink = async () => {
    if (!cardToUnlink) return;
    try {
      await cardApi.unlinkCard(cardToUnlink.linkedCardId);
      await loadAllLinkedCards();
      setShowUnlinkModal(false);
      setCardToUnlink(null);
    } catch (err: any) {
      console.error('Failed to unlink card:', err);
    }
  };

  const resetForm = () => {
    setLoginType('idpw');
    setOrganization('');
    setLoginId('');
    setLoginPw('');
    setLoginTypeLevel('');
    setConnectedId('');
    setAvailableCards([]);
    setSelectedCardIds(new Set());
    // 기본 가계부 다시 선택
    const defaultLedger = ledgers.find((l) => l.isDefault);
    if (defaultLedger) {
      setSelectedLedgerIds(new Set([defaultLedger.ledgerId]));
    } else if (ledgers.length > 0) {
      setSelectedLedgerIds(new Set([ledgers[0].ledgerId]));
    } else {
      setSelectedLedgerIds(new Set());
    }
    setTwoWayInfo(null);
    setError('');
  };

  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '동기화 안됨';
    const date = new Date(dateStr);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // ==================== 렌더링 ====================

  const renderStepIndicator = () => {
    const isStep1Done = step === 'waiting' || step === 'select';
    const isStep1Active = step === 'connect';
    return (
      <div className={styles['step-indicator']}>
        <div className={`${styles['step']} ${isStep1Active ? styles['active'] : ''} ${isStep1Done ? styles['done'] : ''}`}>
          <div className={styles['step-number']}>{isStep1Done ? '✓' : '1'}</div>
          <span className={styles['step-label']}>카드사 연결</span>
        </div>
        {step === 'waiting' && (
          <>
            <div className={`${styles['step-divider']}`} />
            <div className={`${styles['step']} ${styles['active']}`}>
              <div className={styles['step-number']}>📱</div>
              <span className={styles['step-label']}>인증 대기</span>
            </div>
          </>
        )}
        <div className={`${styles['step-divider']} ${step === 'select' ? styles['done'] : ''}`} />
        <div className={`${styles['step']} ${step === 'select' ? styles['active'] : ''}`}>
          <div className={styles['step-number']}>{ step === 'waiting' ? '3' : '2'}</div>
          <span className={styles['step-label']}>카드 선택</span>
        </div>
      </div>
    );
  };

  // Step: 카드사 연결 폼
  const renderConnectForm = () => (
    <div className={styles['connect-section']}>
      {renderStepIndicator()}
      <form className={`${styles['connect-form']} glass`} onSubmit={handleConnect}>
        <h3 className={styles['section-title']}>카드사 계정 연결</h3>

        {error && <div className={styles['error-message']}>{error}</div>}

        {/* 카드사 선택 */}
        <div className={styles['form-row']}>
          <div className={styles['form-group']}>
            <label>카드사 선택</label>
            <select
              value={organization}
              onChange={(e) => setOrganization(e.target.value)}
              required
            >
              <option value="">카드사를 선택하세요</option>
              {Object.entries(ORG_MAP).map(([code, name]) => (
                <option key={code} value={code}>{name}</option>
              ))}
            </select>
          </div>
        </div>

        {/* 인증 방식 선택 */}
        <div className={styles['form-group']}>
          <label>인증 방식</label>
          <div className={styles['auth-type-selector']}>
            <button
              type="button"
              className={`${styles['auth-type-btn']} ${loginType === 'idpw' ? styles['active'] : ''}`}
              onClick={() => setLoginType('idpw')}
            >
              <span className={styles['auth-type-icon']}>🔑</span>
              <span>ID/PW 로그인</span>
            </button>
            <button
              type="button"
              className={`${styles['auth-type-btn']} ${loginType === 'simple' ? styles['active'] : ''}`}
              onClick={() => setLoginType('simple')}
            >
              <span className={styles['auth-type-icon']}>📱</span>
              <span>간편인증</span>
            </button>
          </div>
        </div>

        {/* ID/PW 입력 폼 */}
        {loginType === 'idpw' && (
          <div className={styles['form-row']}>
            <div className={styles['form-group']}>
              <label>카드사 로그인 ID</label>
              <input
                type="text"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
                placeholder="카드사 홈페이지 아이디"
                required
              />
            </div>
            <div className={styles['form-group']}>
              <label>카드사 로그인 비밀번호</label>
              <input
                type="password"
                value={loginPw}
                onChange={(e) => setLoginPw(e.target.value)}
                placeholder="카드사 홈페이지 비밀번호"
                required
              />
            </div>
          </div>
        )}

        {/* 간편인증 수단 선택 */}
        {loginType === 'simple' && (
          <div className={styles['form-group']}>
            <label>간편인증 수단</label>
            <div className={styles['simple-auth-grid']}>
              {SIMPLE_AUTH_METHODS.map((method) => (
                <button
                  key={method.value}
                  type="button"
                  className={`${styles['simple-auth-item']} ${loginTypeLevel === method.value ? styles['active'] : ''}`}
                  onClick={() => setLoginTypeLevel(method.value)}
                >
                  <span className={styles['simple-auth-icon']}>{method.icon}</span>
                  <span className={styles['simple-auth-label']}>{method.label}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        <div className={styles['form-actions']}>
          <button
            type="button"
            className={styles['btn-secondary']}
            onClick={() => { resetForm(); setStep('list'); }}
          >
            취소
          </button>
          <button
            type="submit"
            className={styles['btn-primary']}
            disabled={connecting || (loginType === 'simple' && !loginTypeLevel)}
          >
            {connecting ? '연결 중...' : loginType === 'simple' ? '인증 요청' : '카드사 연결'}
          </button>
        </div>
      </form>
    </div>
  );

  // 간편인증 대기 화면
  const renderWaitingStep = () => {
    const selectedMethod = SIMPLE_AUTH_METHODS.find((m) => m.value === loginTypeLevel);
    return (
      <div className={styles['connect-section']}>
        {renderStepIndicator()}
        <div className={`${styles['waiting-section']} glass`}>
          <div className={styles['waiting-icon']}>📱</div>
          <h3>간편인증 대기 중</h3>
          <p className={styles['waiting-desc']}>
            {selectedMethod ? `${selectedMethod.icon} ${selectedMethod.label}` : '선택한 인증 앱'}에서
            인증 요청을 확인해주세요.
          </p>
          <div className={styles['waiting-spinner']}></div>
          <p className={styles['waiting-hint']}>
            앱에서 인증을 완료한 후 아래 버튼을 눌러주세요.
          </p>

          {error && <div className={styles['error-message']}>{error}</div>}

          <div className={styles['form-actions']}>
            <button
              type="button"
              className={styles['btn-secondary']}
              onClick={() => { resetForm(); setStep('list'); }}
            >
              취소
            </button>
            <button
              type="button"
              className={styles['btn-primary']}
              onClick={handleConfirmSimpleAuth}
              disabled={confirming}
            >
              {confirming ? '확인 중...' : '인증 완료'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Step: 카드 선택
  const renderCardSelect = () => (
    <div className={styles['card-select-section']}>
      {renderStepIndicator()}

      <div className={styles['card-select-header']}>
        <h3 className={styles['section-title']}>보유카드 선택</h3>
        <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          {ORG_MAP[organization] || organization} - {availableCards.length}개 카드 발견
        </span>
      </div>

      {error && <div className={styles['error-message']}>{error}</div>}

      {availableCards.length === 0 ? (
        <div className={`${styles['empty-state']} glass`}>
          <h3>조회된 카드가 없습니다</h3>
          <p>해당 카드사에 등록된 카드가 없거나 조회에 실패했습니다.</p>
        </div>
      ) : (
        <>
          {/* 전체 선택 */}
          <div className={styles['select-all-row']}>
            <label className={styles['select-all-label']} onClick={toggleSelectAll}>
              <div className={`${styles['card-checkbox']} ${selectedCardIds.size === availableCards.length ? styles['checked'] : ''}`}>
                {selectedCardIds.size === availableCards.length && '\u2713'}
              </div>
              <span>전체 선택 ({selectedCardIds.size}/{availableCards.length})</span>
            </label>
          </div>

          <div className={styles['card-select-list']}>
            {availableCards.map((card) => {
              const alreadyLinked = isAlreadyLinked(card);
              return (
                <div
                  key={card.cardId}
                  className={`${styles['card-select-item']} ${selectedCardIds.has(card.cardId) ? styles['selected'] : ''}`}
                  onClick={() => toggleCardSelection(card.cardId)}
                >
                  <div className={`${styles['card-checkbox']} ${selectedCardIds.has(card.cardId) ? styles['checked'] : ''}`}>
                    {selectedCardIds.has(card.cardId) && '\u2713'}
                  </div>
                  <div className={styles['card-select-info']}>
                    <div className={styles['card-select-name']}>
                      {card.cardName}
                      {alreadyLinked && (
                        <span className={styles['already-linked-badge']}>연동됨</span>
                      )}
                    </div>
                    <div className={styles['card-select-number']}>{card.cardNoMasked}</div>
                  </div>
                </div>
              );
            })}
          </div>

          <div className={styles['ledger-select-row']}>
            <label>연동할 가계부:</label>
            <div className={styles['ledger-checkbox-list']}>
              {ledgers.map((ledger) => (
                <label key={ledger.ledgerId} className={styles['ledger-checkbox-item']}>
                  <input
                    type="checkbox"
                    checked={selectedLedgerIds.has(ledger.ledgerId)}
                    onChange={() => {
                      const next = new Set(selectedLedgerIds);
                      if (next.has(ledger.ledgerId)) {
                        next.delete(ledger.ledgerId);
                      } else {
                        next.add(ledger.ledgerId);
                      }
                      setSelectedLedgerIds(next);
                    }}
                  />
                  <span>{ledger.name}</span>
                </label>
              ))}
            </div>
          </div>

          <div className={styles['link-actions']}>
            <button
              className={styles['btn-secondary']}
              onClick={() => { setStep('connect'); setAvailableCards([]); setSelectedCardIds(new Set()); }}
            >
              이전
            </button>
            <button
              className={styles['btn-primary']}
              onClick={handleLinkCards}
              disabled={linking || selectedCardIds.size === 0 || selectedLedgerIds.size === 0}
            >
              {linking ? '연동 중...' : `선택한 카드 연동 (${selectedCardIds.size}개)`}
            </button>
          </div>
        </>
      )}
    </div>
  );

  // 메인: 연동된 카드 목록
  const renderCardList = () => (
    <>
      <div className={styles['page-header']}>
        <div className={styles['page-header-left']}>
          <h1 className="gradient-text">카드 연동</h1>
          <p className={styles['subtitle']}>카드사를 연결하고 거래 내역을 자동으로 가져오세요</p>
        </div>
      </div>

      {/* 액션 바 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            className={styles['btn-back']}
            onClick={() => navigate(-1)}
          >
            &#8592; 거래 내역
          </button>
          {ledgers.length > 1 && (
            <select
              value={filterLedgerId}
              onChange={(e) => {
                const val = Number(e.target.value);
                setFilterLedgerId(val);
                if (val > 0) {
                  setSearchParams({ ledgerId: String(val) });
                } else {
                  setSearchParams({});
                }
              }}
              style={{
                padding: '0.5rem 0.75rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--glass-border)',
                background: 'var(--bg-tertiary)',
                color: 'var(--text-primary)',
                fontSize: '0.9rem',
              }}
            >
              <option value={0}>전체 가계부</option>
              {ledgers.map((l) => (
                <option key={l.ledgerId} value={l.ledgerId}>{l.name}</option>
              ))}
            </select>
          )}
        </div>
        <button
          className={styles['btn-primary']}
          onClick={() => setStep('connect')}
        >
          <span>+</span> 카드사 연결하기
        </button>
      </div>

      {error && <div className={styles['error-message']}>{error}</div>}

      {loading ? (
        <div className={`${styles['loading-state']} glass`}>
          <div className={styles['loading-spinner']} />
          <p>연동 카드를 불러오는 중...</p>
        </div>
      ) : linkedCards.length === 0 ? (
        <div className={`${styles['empty-state']} glass`}>
          <div className={styles['empty-icon']}>&#128179;</div>
          <h3>연동된 카드가 없습니다</h3>
          <p>카드사를 연결하고 카드를 추가해보세요!</p>
        </div>
      ) : (
        <div className={styles['card-grid']}>
          {linkedCards.map((card) => (
            <div key={card.linkedCardId} className={styles['card-item']}>
              <div className={styles['card-header']}>
                <span className={styles['card-org-badge']}>
                  {card.organizationName}
                </span>
                <div className={styles['card-actions-inline']}>
                  <button
                    className={styles['btn-sync']}
                    onClick={() => handleSync(card.linkedCardId)}
                    disabled={syncingId === card.linkedCardId}
                  >
                    {syncingId === card.linkedCardId ? '...' : '\u21BB'} 동기화
                  </button>
                  <button
                    className={styles['btn-unlink']}
                    onClick={() => confirmUnlink(card)}
                  >
                    × 해제
                  </button>
                </div>
              </div>
              <div className={styles['card-name']}>{card.cardName}</div>
              <div className={styles['card-number']}>{card.cardNoMasked}</div>
              <div className={styles['card-meta']}>
                {filterLedgerId === 0 && (
                  <div className={styles['card-ledger-name']}>
                    {ledgers.find((l) => l.ledgerId === card.ledgerId)?.name || `가계부 #${card.ledgerId}`}
                  </div>
                )}
                <div className={styles['sync-info']}>
                  마지막 동기화: {formatDate(card.lastSyncedDate)}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );

  return (
    <div className={styles['linked-cards']}>
      {step === 'list' && renderCardList()}
      {step === 'connect' && renderConnectForm()}
      {step === 'waiting' && renderWaitingStep()}
      {step === 'select' && renderCardSelect()}

      {/* 동기화 결과 토스트 */}
      {syncResult && (
        <div className={styles['sync-toast']}>
          <h4>동기화 완료</h4>
          <p>기간: {syncResult.syncPeriod}</p>
          <p>새로 추가: {syncResult.insertedCount}건</p>
          <p>업데이트: {syncResult.updatedCount}건</p>
          <p>건너뜀: {syncResult.skippedCount}건</p>
        </div>
      )}

      {/* 연동 해제 모달 */}
      {showUnlinkModal && cardToUnlink && (
        <div className={styles['modal-overlay']}>
          <div className={`${styles['modal']} glass`}>
            <h3>카드 연동 해제</h3>
            <p>이 카드의 연동을 해제하시겠습니까?</p>
            <div className={styles['modal-info']}>
              <strong>{cardToUnlink.cardName}</strong><br />
              {cardToUnlink.organizationName} {cardToUnlink.cardNoMasked}<br />
              <small style={{ color: 'var(--accent-red)' }}>
                해당 카드의 거래 내역도 함께 삭제됩니다. (30일 내 복구 가능)
              </small>
            </div>
            <div className={styles['modal-actions']}>
              <button
                className={styles['btn-secondary']}
                onClick={() => { setShowUnlinkModal(false); setCardToUnlink(null); }}
              >
                취소
              </button>
              <button className={styles['btn-danger']} onClick={handleUnlink}>
                연동 해제
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
