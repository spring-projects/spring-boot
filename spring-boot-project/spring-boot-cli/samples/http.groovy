package org.test

import org.springframework.web.client.RestTemplate;

@Controller
class Example implements CommandLineRunner {

	@Autowired
	ApplicationContext context

	@RequestMapping("/")
	@ResponseBody
	public String helloWorld() {
		return "World!"
	}

	void run(String... args) {
		def port = context.webServer.port
		def world = new RestTemplate().getForObject("http://localhost:" + port + "/", String.class);
		print "Hello " + world
	}

}
