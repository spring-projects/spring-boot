package org.test

@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@Grab("org.springframework.bootstrap:spring-bootstrap-service:0.0.1-SNAPSHOT")

@Controller
class SampleController {

	@RequestMapping("/")
	@ResponseBody
	public def hello() {
		[message: "Hello World!"]
	}
}


