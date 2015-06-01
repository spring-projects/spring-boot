package org.test

@Grab("spring-boot-starter-hornetq")
@Grab("hornetq-jms-server")
import java.util.concurrent.CountDownLatch
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl

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
			messageListener: new MessageListenerAdapter(new Receiver(latch:latch)) {{
				defaultListenerMethod = "receive"
			}}
		])
	}

	void run(String... args) {
		def messageCreator = { session ->
			session.createObjectMessage("Greetings from Spring Boot via HornetQ")
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

	@Bean JMSQueueConfigurationImpl springBootQueue() {
		new  JMSQueueConfigurationImpl('spring-boot', null, false)
	}

}
