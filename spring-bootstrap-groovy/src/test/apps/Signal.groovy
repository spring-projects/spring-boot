import java.io.File;

import org.springframework.bootstrap.CommandLineRunner;

@Component
class Signal implements CommandLineRunner {
	
	private File messages = new File("target/messages")
	
	boolean ready = false

	@Override
	void run(String... args) {
		messages.mkdirs()
		new File(messages, "ready").write("Ready!")
		ready = true
	}
}