package com.mgumieniak.spring.kafka.webapp.controller;

import com.mgumieniak.spring.kafka.models.PracticalAdvice;
import com.mgumieniak.spring.kafka.webapp.entities.Post;
import com.mgumieniak.spring.kafka.webapp.repo.PostRepo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaFailureCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HelloKafkaController {

    private final DefaultListableBeanFactory beanFactory;
    private final PostRepo postRepo;
    private final KafkaTemplate<String, Object> template;
    private final KafkaTemplate<String, Object> txTemplate;
    private final String topicName;
    private final String txTopicName;
    private final int messagesPerRequest;
    private final KafkaTemplate<String, Object> txSecTemplate;
    private final String txSecTopicName;

    public HelloKafkaController(
            DefaultListableBeanFactory beanFactory, PostRepo postRepo,
            @Qualifier("kafkaTemplate") final KafkaTemplate<String, Object> template,
            @Qualifier("transactionKafkaTemplate") KafkaTemplate<String, Object> txTemplate,
            @Value("${tpd.topic-name}") final String topicName,
            @Value("${tpd.tx-topic-name}") String txTopicName,
            @Value("${tpd.messages-per-request}") final int messagesPerRequest,
            @Qualifier("txSecKafkaTemplate") KafkaTemplate<String, Object> txSecTemplate,
            @Value("${tpd.tx-sec-topic-name}") String txSecTopicName) {
        this.beanFactory = beanFactory;
        this.postRepo = postRepo;
        this.template = template;
        this.txTemplate = txTemplate;
        this.topicName = topicName;
        this.txTopicName = txTopicName;
        this.messagesPerRequest = messagesPerRequest;
        this.txSecTemplate = txSecTemplate;
        this.txSecTopicName = txSecTopicName;
    }

    @GetMapping("/")
    public void healthCheck() {
    }

    @GetMapping("/hello")
    public String hello() {

        for (int i = 0; i < messagesPerRequest; i++) {
            this.template.send(topicName, String.valueOf(i),
                    new PracticalAdvice("A Practical Advice", i));
        }
        log.info("All messages received");
        return "Hello Kafka!";
    }

    @Transactional("transactionManager")
    @GetMapping("/tx")
    public String transaction() {

        for (int i = 0; i < 1; i++) {
            this.txTemplate.send(txTopicName, String.valueOf(i), new PracticalAdvice("A Practical Advice", i));
        }

        return "Hello tx kafka!";
    }

    private void createPost() {
        val post = new Post();
        post.setName("Test");
        postRepo.save(post);
    }

    private KafkaFailureCallback<String, Object> handleFailure() {
        return ex -> log.error("Failed result: {}", ex.getFailedProducerRecord());
    }

    private SuccessCallback<SendResult<String, Object>> handleSuccess() {
        return result -> log.info("SendResult: {}", result);
    }

    @KafkaListener(topics = "advice-topic")
    public void listenAsObject(PracticalAdvice payload, Acknowledgment ack) {
        log.info("Received advice-topic: {}", payload);

        ack.acknowledge();
    }

    @Transactional("transactionManager")
    @KafkaListener(containerFactory = "txKafkaListenerContainerFactory", topics = "transaction-advice-topic")
    public void txListenAsObject(PracticalAdvice payload, ConsumerRecordMetadata metadata) {
        log.info("Offset: {} Tx_received: {}", metadata.offset(), payload);

        val post = postRepo.findById(1000L);
        post.get().setName("tx :(");

        this.txSecTemplate.send(txSecTopicName, String.valueOf(777), new PracticalAdvice("A Practical Advice", 777));

        throw new RuntimeException("This is a test exception");
    }

    @KafkaListener(containerFactory = "txSecKafkaListenerContainerFactory", topics = "tx-sec-topic")
    public void listenSecondTxTopic(PracticalAdvice payload) {
        log.info("Received tx-sec-topic: {}", payload);
    }
}
