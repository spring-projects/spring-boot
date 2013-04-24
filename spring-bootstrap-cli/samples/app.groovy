package org.test

@GrabResolver(name='spring-milestone', root='http://repo.springframework.org/milestone')
@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@Grab("org.springframework.bootstrap:spring-bootstrap:0.0.1-SNAPSHOT")
@Grab("org.springframework:spring-context:4.0.0.BOOTSTRAP-SNAPSHOT")

@org.springframework.bootstrap.context.annotation.EnableAutoConfiguration
@org.springframework.stereotype.Component
class Example implements org.springframework.bootstrap.CommandLineRunner {

	@org.springframework.beans.factory.annotation.Autowired
	private MyService myService;

	public void run(String... args) {
		print "Hello " + this.myService.sayWorld();
	}
}


@org.springframework.stereotype.Service
class MyService {

	public String sayWorld() {
		return "World!";
	}
}


