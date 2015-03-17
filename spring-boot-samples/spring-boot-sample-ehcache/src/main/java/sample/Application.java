package sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableCaching
public class Application implements CommandLineRunner {

	@Autowired
	private CountryService countryService;

	public static void main(String... args) {
		SpringApplication.run(Application.class);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println(countryService.countries());
		System.out.println(countryService.countries());
	}
}


interface CountryService {

	List<String> countries();

}


@Service
class CountryServiceImpl implements CountryService {

	@Cacheable("countries")
	public List<String> countries() {
		System.out.println("Loading countries");
		return Arrays.asList("Peru", "United States");
	}

}
