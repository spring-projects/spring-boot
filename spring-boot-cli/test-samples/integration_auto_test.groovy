import org.springframework.jms.core.JmsTemplate

@SpringBootTest(classes=JmsExample)
class JmsTests {

	@Autowired
	JmsTemplate jmsTemplate

	@Test
	void test() {
		assertNotNull(jmsTemplate)
	}

}
