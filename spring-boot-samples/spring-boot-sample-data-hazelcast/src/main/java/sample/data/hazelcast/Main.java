package sample.data.hazelcast;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.hazelcast.repository.config.EnableHazelcastRepositories;
import org.springframework.util.Assert;

import sample.data.hazelcast.domain.Zodiac;
import sample.data.hazelcast.service.ZodiacService;

/**
 * <P>A main class to find the star sign for a random longitude.
 * </P>
 * <P>Run this from the command line with:
 * <P>
 * <UL>
 * <LI>{@code java -jar target/spring-boot-sample-data-hazelcast-*.jar}</LI>
 * </UL>
 * <P>or</P>
 * <UL>
 * <LI>{@code mvn spring-boot:run}</LI>
 * </UL>
 * <P>As part of this a standalone Hazelcast instance is started, and this
 * will log various messages. You can ignore the warning.
 * "{@code No join method is enabled! Starting standalone.}"
 * </P>
 * 
 * @author Neil Stevenson
 */
@SpringBootApplication
@EnableHazelcastRepositories
public class Main implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
		System.exit(0);
	}

	@Autowired
	private ZodiacService zodiacService;
	
	/**
	 * <P>Look up the start sign for a random degee in the Sky,
	 * from 0 to 359 (as 360 degrees).
	 * </P>
	 * 
	 * @param Not used
	 */
	@Override
	public void run(String... args) throws Exception {

		int longitude = new Random().nextInt(360);
		
		Zodiac zodiac = this.zodiacService.findByLongitude(longitude);

		Assert.notNull(zodiac, Zodiac.class.getSimpleName());
		
		System.out.println("***************************************************************************************************");
		System.out.println("***************************************************************************************************");
		System.out.println("Starsign for " + longitude + " is " + zodiac);
		System.out.println("***************************************************************************************************");
		System.out.println("***************************************************************************************************");
	}

}
