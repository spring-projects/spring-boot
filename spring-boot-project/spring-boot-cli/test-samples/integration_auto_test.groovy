@SpringBootTest(classes=App)
class AppTests {

	@Autowired
	MyService myService

	@Test
	void test() {
		assertNotNull(myService)
	}

}
