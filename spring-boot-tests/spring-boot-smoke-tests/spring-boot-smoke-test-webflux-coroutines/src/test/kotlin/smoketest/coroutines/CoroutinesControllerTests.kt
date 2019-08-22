package smoketest.coroutines

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.*
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CoroutinesControllerTests(@Autowired private val webClient: WebTestClient) {

	@Test
	fun testSuspendingFunction() {
		webClient.get().uri("/suspending").accept(MediaType.TEXT_PLAIN).exchange()
				.expectBody<String>().isEqualTo("Hello World")
	}

	@Test
	fun testFlow() {
		webClient.get().uri("/flow").accept(MediaType.TEXT_PLAIN).exchange()
				.expectBody<String>().isEqualTo("Hello World")
	}

}
