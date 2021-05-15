package org.test

import java.util.concurrent.CountDownLatch

@Log
@Configuration(proxyBeanMethods = false)
@EnableRabbit
class RabbitExample implements CommandLineRunner {

	private CountDownLatch latch = new CountDownLatch(1)

	@Autowired
	RabbitTemplate rabbitTemplate

	void run(String... args) {
		log.info "Sending RabbitMQ message..."
		rabbitTemplate.convertAndSend("spring-boot", "Greetings from Spring Boot via RabbitMQ")
		latch.await()
	}

	@RabbitListener(queues = 'spring-boot')
	def receive(String message) {
		log.info "Received ${message}"
		latch.countDown()
	}

	@Bean
	org.springframework.amqp.core.Queue queue() {
		new org.springframework.amqp.core.Queue("spring-boot", false)
	}

}