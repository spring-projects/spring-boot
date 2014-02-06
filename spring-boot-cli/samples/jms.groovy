package org.test

@Grab("org.apache.activemq:activemq-all:5.4.0")
@Grab("activemq-pool")
import java.util.concurrent.CountDownLatch

@Log
@Configuration
@EnableJmsMessaging
class JmsExample implements CommandLineRunner {

	private CountDownLatch latch = new CountDownLatch(1)

	@Autowired
	JmsTemplate jmsTemplate

	@Bean
	DefaultMessageListenerContainer jmsListener(ConnectionFactory connectionFactory) {
		new DefaultMessageListenerContainer([
			connectionFactory: connectionFactory,
			destinationName: "spring-boot",
			pubSubDomain: true,
			messageListener: new MessageListenerAdapter(new Receiver(latch:latch)) {{
				defaultListenerMethod = "receive"
			}}
		])
	}

	void run(String... args) {
		def messageCreator = { session ->
			session.createObjectMessage("Greetings from Spring Boot via ActiveMQ")
		} as MessageCreator
		log.info "Sending JMS message..."
		jmsTemplate.send("spring-boot", messageCreator)
		log.info "Send JMS message, waiting..."
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
