package com.mgumieniak.spring.kafka.webapp.configure.txSec;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class TxSecProducerConfiguration {

    private final KafkaProperties kafkaProperties;

    @Value("${tpd.tx-sec-prefix}")
    private String txSecPrefix;

    @Value("${tpd.tx-sec-topic-name}")
    private String txSecTopicName;


    @Bean
    public Map<String, Object> txSecProducerConfigs() {
        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "transactional_sec_producer");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return props;
    }

    @Bean
    public ProducerFactory<String, Object> txSecProducerFactory() {
        DefaultKafkaProducerFactory<String, Object> kafkaProducerFactory =  new DefaultKafkaProducerFactory<>(txSecProducerConfigs());
        kafkaProducerFactory.setTransactionIdPrefix(txSecPrefix);
        return kafkaProducerFactory;
    }

    @Bean
    public KafkaTransactionManager<String, Object> txSecKafkaTransactionManager(){
        return new KafkaTransactionManager<>(txSecProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> txSecKafkaTemplate() {
        return new KafkaTemplate<>(txSecProducerFactory());
    }

    @Bean
    public NewTopic txSecAdviceTopic() {
        return TopicBuilder.name(txSecTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

}
