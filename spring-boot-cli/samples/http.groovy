package org.test

@Grab("org.codehaus.groovy.modules.http-builder:http-builder:0.5.2") // This one just to test dependency resolution
import groovyx.net.http.*

@Controller
class Example implements CommandLineRunner {

	@RequestMapping("/")
	@ResponseBody
	public String helloWorld() {
		return "World!"
	}

	void run(String... args) {
		def world = new RESTClient("http://localhost:8080").get(path:"/").data.text
		print "Hello " + world
	}
}
