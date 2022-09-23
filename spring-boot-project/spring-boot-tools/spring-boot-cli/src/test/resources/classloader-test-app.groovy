import org.springframework.util.*

@Component
public class Test implements CommandLineRunner {

	public void run(String... args) throws Exception {
		println "HasClasses-" + ClassUtils.isPresent("missing", null) + "-" +
			ClassUtils.isPresent("org.springframework.boot.SpringApplication", null) + "-" +
			ClassUtils.isPresent(args[0], null)
	}

}

