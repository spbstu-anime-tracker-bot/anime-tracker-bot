package com.animetracker.config;

import com.animetracker.recommendation.RecommendationRequestEvent;
import com.animetracker.recommendation.RecommendationResultEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.recommendation-requests}")
    private String requestsTopic;

    @Value("${kafka.topics.recommendation-results}")
    private String resultsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    public NewTopic recommendationRequestsTopic() {
        return TopicBuilder.name(requestsTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic recommendationResultsTopic() {
        return TopicBuilder.name(resultsTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public ConsumerFactory<String, RecommendationRequestEvent> requestConsumerFactory() {
        Map<String, Object> props = baseConsumerProps("anime-tracker-worker");
        JsonDeserializer<RecommendationRequestEvent> deserializer =
                new JsonDeserializer<>(RecommendationRequestEvent.class, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RecommendationRequestEvent> requestListenerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RecommendationRequestEvent>();
        factory.setConsumerFactory(requestConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, RecommendationResultEvent> resultConsumerFactory() {
        Map<String, Object> props = baseConsumerProps("anime-tracker-group");
        JsonDeserializer<RecommendationResultEvent> deserializer =
                new JsonDeserializer<>(RecommendationResultEvent.class, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RecommendationResultEvent> resultListenerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RecommendationResultEvent>();
        factory.setConsumerFactory(resultConsumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
