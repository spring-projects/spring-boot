package org.test

@EnableAuthorizationServer
@EnableResourceServer
@RestController
class SampleController {

	@RequestMapping("/")
	def hello() {
		[message: "Hello World!"]
	}

}
