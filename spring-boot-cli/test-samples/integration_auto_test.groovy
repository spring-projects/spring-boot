@SpringApplicationConfiguration(ReactorApplication)
@IntegrationTest('server.port:0')
class RestTests {

	@Autowired
	EventBus eventBus

	@Test
	void test() {
		assertNotNull(eventBus)
	}

}
