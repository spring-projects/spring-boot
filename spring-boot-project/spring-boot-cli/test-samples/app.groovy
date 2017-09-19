@Configuration
class App {

	@Bean
	MyService myService() {
		return new MyService()
	}

}

class MyService {

	String sayWorld() {
		return "World!"
	}

}