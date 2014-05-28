@SpringApplicationConfiguration(classes=ReactorApplication)
@IntegrationTest('server.port:0')
class RestTests {

	@Autowired
	Reactor reactor

	@Test
	void test() {
		assertNotNull(reactor)
	}

}
