package com.hamkkebu.transactionservice.codef.config;

import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Codef API 설정
 *
 * <p>EasyCodef 객체를 싱글톤 Bean으로 관리합니다.</p>
 * <p>환경변수 또는 application.yml에서 설정값을 주입받습니다.</p>
 */
@Slf4j
@Getter
@Configuration
public class CodefConfig {

    @Value("${codef.client-id:}")
    private String clientId;

    @Value("${codef.client-secret:}")
    private String clientSecret;

    @Value("${codef.demo-client-id:}")
    private String demoClientId;

    @Value("${codef.demo-client-secret:}")
    private String demoClientSecret;

    @Value("${codef.public-key:}")
    private String publicKey;

    /**
     * 서비스 타입 (SANDBOX, DEMO, API)
     * <p>기본값: DEMO (실제 금융기관 데이터 조회 가능, 일 100회 무료)</p>
     */
    @Value("${codef.service-type:DEMO}")
    private String serviceType;

    @Bean
    public EasyCodef easyCodef() {
        EasyCodef codef = new EasyCodef();

        // 데모 환경 설정
        if (demoClientId != null && !demoClientId.isEmpty()) {
            codef.setClientInfoForDemo(demoClientId, demoClientSecret);
            log.info("[Codef] Demo client configured");
        }

        // 정식 환경 설정
        if (clientId != null && !clientId.isEmpty()) {
            codef.setClientInfo(clientId, clientSecret);
            log.info("[Codef] Production client configured");
        }

        // RSA 공개키 설정
        if (publicKey != null && !publicKey.isEmpty()) {
            // PEM 헤더/푸터 및 공백/줄바꿈 제거 (순수 Base64만 전달)
            String cleanKey = publicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            codef.setPublicKey(cleanKey);
            log.info("[Codef] Public key configured (length={})", cleanKey.length());
        }

        log.info("[Codef] Initialized with serviceType={}", serviceType);
        return codef;
    }

    /**
     * 현재 설정된 서비스 타입 반환
     */
    public EasyCodefServiceType getEasyCodefServiceType() {
        return switch (serviceType.toUpperCase()) {
            case "API" -> EasyCodefServiceType.API;
            case "SANDBOX" -> EasyCodefServiceType.SANDBOX;
            default -> EasyCodefServiceType.DEMO;
        };
    }
}
