package org.test

@Component
@EnableIntegration
class SpringIntegrationExample implements CommandLineRunner {

	@Bean
	DirectChannel input() {
		new DirectChannel();
	}

	@Override
	void run(String... args) {
		print new MessagingTemplate(input()).convertSendAndReceive("World")
	}
}

@MessageEndpoint
class HelloTransformer {

	@Transformer(inputChannel="input")
	String transform(String payload) {
		"Hello, ${payload}"
	}

}
