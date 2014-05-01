package org.test

@Configuration
@EnableIntegration
class SpringIntegrationExample implements CommandLineRunner {

	@Bean
	DirectChannel input() {
		new DirectChannel();
	}

	@Override
	void run(String... args) {
		println new MessagingTemplate(input()).convertSendAndReceive("World", String)
	}
}

@MessageEndpoint
class HelloTransformer {

	@Transformer(inputChannel="input")
	String transform(String payload) {
		"Hello, ${payload}"
	}

}
