@SpringBootTest(classes=Application)
class BookTests {
	@Autowired
	Book book
	@Test
	void testBooks() {
		assertEquals("Tom Clancy", book.author)
	}
}

@Configuration(proxyBeanMethods = false)
class Application {
	@Bean
	Book book() {
		new Book(author: "Tom Clancy", title: "Threat Vector")
	}
}

class Book {
	String author
	String title
}
