package com.hamkkebu.transactionservice.config;

import com.hamkkebu.boilerplate.common.user.config.AbstractUserEventKafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Service 전용 Kafka Consumer 설정
 *
 * <p>AbstractUserEventKafkaConfig를 상속받아 공통 로직을 재사용합니다.</p>
 * <p>사용자 이벤트(USER_REGISTERED, USER_DELETED)를 Map으로 수신하여
 * 이벤트 타입에 따라 처리합니다.</p>
 *
 * <p>가계부/공유 이벤트는 Outbox 패턴(StringSerializer)으로 전송되므로
 * StringDeserializer 기반 별도 factory를 사용합니다.</p>
 */
@Configuration("transactionUserEventKafkaConfig")
public class KafkaConfig extends AbstractUserEventKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:transaction-service-group}")
    private String groupId;

    @Override
    protected String getBootstrapServers() {
        return bootstrapServers;
    }

    @Override
    protected String getGroupId() {
        return groupId;
    }

    @Bean
    public ConsumerFactory<String, Map<String, Object>> transactionUserEventConsumerFactory() {
        return createConsumerFactory();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> transactionKafkaListenerContainerFactory() {
        return createContainerFactory();
    }

    /**
     * Outbox 패턴 이벤트용 Consumer Factory (StringDeserializer)
     *
     * <p>ledger-service의 OutboxEventScheduler가 StringSerializer로 전송하므로
     * StringDeserializer로 수신 후 수동 JSON 파싱합니다.</p>
     */
    @Bean
    public ConsumerFactory<String, String> outboxEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Outbox 패턴 이벤트용 Listener Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> outboxEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(outboxEventConsumerFactory());
        return factory;
    }
}
