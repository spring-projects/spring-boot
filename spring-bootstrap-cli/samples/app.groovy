package org.test

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	void run(String... args) {
		print "Hello " + this.myService.sayWorld()
	}
}


@Service
class MyService {

	String sayWorld() {
		return "World!"
	}
}


