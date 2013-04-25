package org.test

@Component
class Example implements CommandLineRunner {

	@Autowired
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


