package org.test

@Configuration
@EnableIntegration
class SpringIntegrationExample implements CommandLineRunner {

	@Autowired
	private ApplicationContext context;

	@Bean
	DirectChannel input() {
		new DirectChannel();
	}

	@Override
	void run(String... args) {
		println()
		println '>>>> ' + new MessagingTemplate(input()).convertSendAndReceive("World", String) + ' <<<<'
		println()
		/*
		 *  Since this is a simple application that we want to exit right away,
		 *  close the context. For an active integration application, with pollers
		 *  etc, you can either suspend the main thread here (e.g. with System.in.read()),
		 *  or exit the run() method without closing the context, and stop the
		 *  application later using some other technique (kill, JMX etc).
		 */
		context.close()
	}
}

@MessageEndpoint
class HelloTransformer {

	@Transformer(inputChannel="input")
	String transform(String payload) {
		"Hello, ${payload}"
	}

}
