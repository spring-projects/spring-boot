package org.springframework.boot.docs.testing.testcontainers.beandeclaration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MongoDBContainer

@SpringBootTest
@Import(BeanDeclarationConfig::class)
class SpringTest {
	@Autowired
	private val mongo: MongoDBContainer? = null

	@Test
	fun doTest() {
		println("Mongo db is running: " + mongo!!.isRunning)
	}
}