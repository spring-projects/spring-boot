package org.test

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	void run(String... args) {
		println "Hello ${this.myService.sayWorld()}"
		println getClass().getResource('/static/test.txt')
	}
}

@Service
class MyService {

	String sayWorld() {
		return 'World!'
	}
}