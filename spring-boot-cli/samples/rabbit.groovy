package org.test

import java.util.concurrent.CountDownLatch

@Log
@Configuration
@EnableRabbitMessaging
class RabbitExample implements CommandLineRunner {

    private CountDownLatch latch = new CountDownLatch(1)

    @Autowired
    RabbitTemplate rabbitTemplate

    private String queueName = "spring-boot"

    @Bean
    Queue queue() {
        new Queue(queueName, false)
    }

    @Bean
    TopicExchange exchange() {
        new TopicExchange("spring-boot-exchange")
    }

    /**
     * The queue and topic exchange cannot be inlined inside this method and have
     * dynamic creation with Spring AMQP work properly.
     */
    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("spring-boot")
    }

    @Bean
    SimpleMessageListenerContainer container(CachingConnectionFactory connectionFactory) {
        return new SimpleMessageListenerContainer(
            connectionFactory: connectionFactory,
            queueNames: [queueName],
            messageListener: new MessageListenerAdapter(new Receiver(latch:latch), "receive")
        )
    }

    void run(String... args) {
        log.info "Sending RabbitMQ message..."
        rabbitTemplate.convertAndSend(queueName, "Greetings from Spring Boot via RabbitMQ")
        latch.await()
    }

}

@Log
class Receiver {
    CountDownLatch latch

    def receive(String message) {
        log.info "Received ${message}"
        latch.countDown()
    }
}
