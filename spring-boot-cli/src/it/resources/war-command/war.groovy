package org.test

@RestController
class WarExample implements CommandLineRunner {

	@RequestMapping("/")
	public String hello() {
		return "Hello"
	}

	void run(String... args) {
		println getClass().getResource('/org/apache/tomcat/InstanceManager.class')
		println getClass().getResource('/root.properties')
		throw new RuntimeException("onStart error")
	}

}
