package kr.co.seoulit.his.support.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    /**
     * ✅ Retry 후 실패하면 <originalTopic>.dlq 로 이동
     * - FixedBackOff(1s, 5회) : 1초 간격으로 5번 재시도
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (ConsumerRecord<?, ?> r, Exception e) -> new TopicPartition(r.topic() + ".dlq", r.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 5));
    }
}
