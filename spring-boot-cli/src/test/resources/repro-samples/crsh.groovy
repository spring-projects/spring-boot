package org.test

@Grab("spring-boot-starter-remote-shell")

@RestController
class SampleController {

	@RequestMapping("/")
	public def hello() {
		[message: "Hello World"]
	}
}


