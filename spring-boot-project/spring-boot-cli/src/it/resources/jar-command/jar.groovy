package org.test

@EnableGroovyTemplates
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
		println getClass().getResource('/root.properties')
		println template('template.txt', [world:'Mama'])
	}
}

@Service
class MyService {

	String sayWorld() {
		return 'World!'
	}
}