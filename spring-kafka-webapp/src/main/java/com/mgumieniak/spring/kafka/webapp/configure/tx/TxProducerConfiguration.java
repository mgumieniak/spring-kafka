package com.mgumieniak.spring.kafka.webapp.configure.tx;

import lombok.RequiredArgsConstructor;
import lombok.val;
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
public class TxProducerConfiguration {

    private final KafkaProperties kafkaProperties;

    @Value("${tpd.tx-prefix}")
    private String txPrefix;

    @Value("${tpd.tx-topic-name}")
    private String txTopicName;

    @Bean
    public Map<String, Object> transactionProducerConfigs() {
        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "transactional_producer");
//        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx_conf-");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return props;
    }

    @Bean
    public ProducerFactory<String, Object> transactionProducerFactory() {
        DefaultKafkaProducerFactory<String, Object> kafkaProducerFactory =  new DefaultKafkaProducerFactory<>(transactionProducerConfigs());
        kafkaProducerFactory.setTransactionIdPrefix(txPrefix);
        return kafkaProducerFactory;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf){
        return new JpaTransactionManager(emf);
    }

    @Bean
    public KafkaTransactionManager<String, Object> txKafkaTransactionManager(){
        return new KafkaTransactionManager<>(transactionProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> transactionKafkaTemplate() {
        return new KafkaTemplate<>(transactionProducerFactory());
    }

    @Bean
    public NewTopic transactionAdviceTopic() {
        return TopicBuilder.name(txTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

}
