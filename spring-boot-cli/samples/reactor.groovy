package org.test

import java.util.concurrent.CountDownLatch;

@EnableReactor
@Consumer
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

	@Selector(value="hello")
	void receive(String data) {
		log.info "Hello ${data}"
		latch.countDown()
	}
}
