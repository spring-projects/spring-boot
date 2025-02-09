package org.springframework.boot.docs.testing.testcontainers.beandeclaration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName


@TestConfiguration(proxyBeanMethods = false)
internal class BeanDeclarationConfig {
	@ServiceConnection
	@Bean
	fun container(): MongoDBContainer {
		return MongoDBContainer(DockerImageName.parse("mongo:latest"))
	}
}