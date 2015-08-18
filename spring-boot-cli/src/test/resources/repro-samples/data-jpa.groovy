@Grab('spring-boot-starter-data-jpa')
@Grab('h2')
class Sample implements CommandLineRunner {

	// No Data JPA-based logic. We just want to check that the dependencies are
	// resolved correctly and that the app runs

	@Override
	void run(String... args) {
		println "Hello World"
	}

}
