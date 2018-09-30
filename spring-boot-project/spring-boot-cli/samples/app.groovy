package org.test

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	void run(String... args) {
		println "Hello ${this.myService.sayWorld()} From ${getClass().getClassLoader().getResource('samples/app.groovy')}" 
	}
}


@Service
class MyService {

	String sayWorld() {
		return "World!"
	}
}


