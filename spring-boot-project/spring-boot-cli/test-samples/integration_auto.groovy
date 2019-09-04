package com.example

@SpringBootApplication
@SpringBootTest(classes=RestTests, webEnvironment=WebEnvironment.RANDOM_PORT)
class RestTests {

	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	void testHome() {
		assertEquals('Hello', testRestTemplate.getForObject('/', String))
	}

	@RestController
	static class Application {
		@RequestMapping('/')
		String hello() { 'Hello' }
	}

}
