package org.test

import java.util.concurrent.CountDownLatch;

@EnableReactor
@Log
class Runner implements CommandLineRunner {
	
	@Autowired
	Reactor reactor
	
	private CountDownLatch latch = new CountDownLatch(1)
	
	@PostConstruct
	void init() {
		log.info "Registering consumer"
		reactor.on(Selectors.$("hello"), [
			accept: { 
				log.info("Hello ${it.data}")
				latch.countDown()
			}
		] as Consumer)
	}

	void run(String... args) {
		reactor.notify("hello", Event.wrap("Phil"))
		log.info "Notified Phil"
		latch.await()
	}
	
	// @On(reactor="reactor", selector="hello")
	void receive(Event<String> event) {
		log.info "Hello ${event.data}"
	} 
}


