package org.test

@Grab("org.springframework.bootstrap:spring-bootstrap-service:0.0.1-SNAPSHOT")
@Grab("org.springframework.integration:spring-integration-dsl-groovy-amqp:1.0.0.M1")

@Component
@EnableIntegrationPatterns
class SpringIntegrationExample implements CommandLineRunner {

	@Bean
	MessageFlow flow(ApplicationContext context) {
		def builder = new IntegrationBuilder(context)
		builder.messageFlow { transform {"Hello, $it!"} }
	}

	@Override
	void run(String... args) {
		print flow().sendAndReceive("World")
	}
}
