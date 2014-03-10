package org.test

@Component
class Example implements CommandLineRunner {

	@Autowired
	private MyService myService

	void run(String... args) {
		println "Hello ${this.myService.sayWorld()}"
		println getClass().getResource('/public/public.txt')
		println getClass().getResource('/resources/resource.txt')
		println getClass().getResource('/static/static.txt')
		println getClass().getResource('/templates/template.txt')
	}
}

@Service
class MyService {

	String sayWorld() {
		return 'World!'
	}
}