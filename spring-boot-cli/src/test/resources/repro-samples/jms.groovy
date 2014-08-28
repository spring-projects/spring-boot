package org.test

@Grab("org.apache.activemq:activemq-all:5.4.0")
@Grab("activemq-pool")
import java.util.concurrent.CountDownLatch

@Log
@EnableJms
class SampleJmsListener implements CommandLineRunner {

	private CountDownLatch latch = new CountDownLatch(1)

	@Autowired
	JmsTemplate jmsTemplate

	void run(String... args) {
		def messageCreator = { session ->
			session.createObjectMessage("Hello World")
		} as MessageCreator
		log.info "Sending JMS message..."
		jmsTemplate.send("testQueue", messageCreator)
		log.info "Sent JMS message, waiting..."
		latch.await()
	}

	@JmsListener(destination = 'testQueue')
	def receive(String message) {
		log.info "Received ${message}"
		latch.countDown()
	}
}


