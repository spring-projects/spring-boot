@SpringBootTest(classes=ReactorApplication, webEnvironment=WebEnvironment.RANDOM_PORT)
class RestTests {

	@Autowired
	EventBus eventBus

	@Test
	void test() {
		assertNotNull(eventBus)
	}

}
