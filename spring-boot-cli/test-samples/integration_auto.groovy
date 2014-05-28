@SpringApplicationConfiguration(classes=Application)
@IntegrationTest('server.port:0')
@WebAppConfiguration
@DirtiesContext
class RestTests {

	@Value('${local.server.port}')
	int port

	@Test
	void testHome() {
		assertEquals('Hello', new TestRestTemplate().getForObject('http://localhost:' + port, String))
	}

	@RestController
	static class Application {
		@RequestMapping('/')
		String hello() { 'Hello' }
	}
}
