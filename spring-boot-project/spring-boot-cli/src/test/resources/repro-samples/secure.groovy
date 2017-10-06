package org.test

// No security features added just a test that the dependencies are resolved
@Grab("spring-boot-starter-security")

@Controller
class Sample implements CommandLineRunner {

	@Override
	void run(String... args) {
		println "Hello World"
	}
}


