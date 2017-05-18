package org.test

@EnableRetry
@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	void run(String... args) {
		println "Hello ${this.myService.sayWorld()} From ${getClass().getClassLoader().getResource('samples/retry.groovy')}"
	}
}


@Service
class MyService {

	static int count = 0

	@Retryable
	String sayWorld() {
		if (count++==0) {
			throw new IllegalStateException("Planned")
		}
		return "World!"
	}
}
