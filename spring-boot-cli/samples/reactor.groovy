package org.test

@EnableReactor
@Log
class Runner implements CommandLineRunner {
	
	@Autowired
	Reactor reactor

	void run(String... args) {
		reactor.notify("hello", Event.wrap("Phil"))
		log.info "Notified Phil"
	}
	
	@On(reactor="reactor", selector="hello")
	void receive(Event<String> event) {
		log.info "Hello ${event.data}"
	} 
}


