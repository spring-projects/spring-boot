package org.test

@Grab("org.codehaus.groovy.modules.http-builder:http-builder:0.5.2") // This one just to test dependency resolution
import groovyx.net.http.*

@Controller
class Example implements CommandLineRunner {

	@Autowired
	ApplicationContext context;

	@RequestMapping("/")
	@ResponseBody
	public String helloWorld() {
		return "World!"
	}

	void run(String... args) {
		def port = context.webServer.port;
		def world = new RESTClient("http://localhost:" + port).get(path:"/").data.text
		print "Hello " + world
	}

}
