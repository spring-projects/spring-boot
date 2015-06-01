package org.test

import java.util.concurrent.CountDownLatch

@EnableReactor
@Log
class Runner implements CommandLineRunner {

	@Autowired
	Reactor reactor

	private CountDownLatch latch = new CountDownLatch(1)

	@PostConstruct
	void init() {
		log.info "Registering consumer"
	}

	void run(String... args) {
		reactor.notify("hello", Event.wrap("Phil"))
		log.info "Notified Phil"
		latch.await()
	}

	@Bean
	CountDownLatch latch() {
		latch
	}

}

@Consumer
@Log
class Greeter {

	@Autowired
	Reactor reactor

	@Autowired
	private CountDownLatch latch

	@Selector(value="hello")
	void receive(String data) {
		log.info "Hello ${data}"
		latch.countDown()
	}

}