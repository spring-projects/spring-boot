package org.test

@Component
class Example implements org.springframework.bootstrap.CommandLineRunner {

	@org.springframework.beans.factory.annotation.Autowired
	private MyService myService;

	public void run(String... args) {
		print "Hello " + this.myService.sayWorld();
	}
}


@Service
class MyService {

	public String sayWorld() {
		return "World!";
	}
}


