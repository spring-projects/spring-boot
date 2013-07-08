package org.test

import static org.springframework.cli.template.GroovyTemplate.template;

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

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


