package org.test

// No security features added just a test that the dependencies are resolved
@Grab("spring-boot-starter-security")

@RestController
class SampleController {

	@RequestMapping("/")
	public def hello() {
		[message: "Hello World"]
	}
}


