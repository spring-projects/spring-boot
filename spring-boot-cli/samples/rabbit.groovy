package org.test

import java.util.concurrent.CountDownLatch

@Log
@Configuration
@EnableRabbitMessaging
class RabbitExample implements CommandLineRunner {

    private CountDownLatch latch = new CountDownLatch(1)

    @Autowired
    RabbitTemplate rabbitTemplate

    @Bean
    Binding binding() {
        BindingBuilder
                .bind(new Queue("spring-boot", false))
                .to(new TopicExchange("spring-boot-exchange"))
                .with("spring-boot")
    }

    @Bean
    SimpleMessageListenerContainer container(CachingConnectionFactory connectionFactory) {
        return new SimpleMessageListenerContainer(
            connectionFactory: connectionFactory,
            queueNames: ["spring-boot"],
            messageListener: new MessageListenerAdapter(new Receiver(latch:latch), "receive")
        )
    }

    void run(String... args) {
        log.info "Sending RabbitMQ message..."
        rabbitTemplate.convertAndSend("spring-boot", "Greetings from Spring Boot via RabbitMQ")
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
