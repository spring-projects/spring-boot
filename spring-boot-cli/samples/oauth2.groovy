package org.test

@EnableAuthorizationServer
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RestController
class SampleController {

	@PreAuthorize("#oauth2.hasScope('read')")
	@RequestMapping("/")
	def hello() {
		[message: "Hello World!"]
	}

}
