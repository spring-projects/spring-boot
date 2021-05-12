package org.test

import static org.springframework.boot.groovy.GroovyTemplate.*

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	@Override
	void run(String... args) {
		print template("test.txt", ["message":myService.sayWorld()])
	}
}


@Service
class MyService {

	String sayWorld() {
		return "World"
	}
}
