package sample.data.hazelcast.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.hazelcast.repository.config.EnableHazelcastRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import sample.data.hazelcast.config.ZodiacConfig;
import sample.data.hazelcast.config.ZodiacData;
import sample.data.hazelcast.domain.Zodiac;
import sample.data.hazelcast.repository.ZodiacRepository;

/**
 * <P>{@code @Service} not {@code @Repository} tests, so business
 * logic tests.
 * </P>
 * 
 * @author Neil Stevenson
 */
@EnableHazelcastRepositories(basePackageClasses=ZodiacRepository.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes={ZodiacConfig.class, ZodiacData.class, ZodiacService.class})
public class ZodiacServiceTests {

	@Autowired
	private ZodiacService zodiacService;
	
	/** <P>Scorpio runs from 210 to 239.
	 * </P>
	 */
	@Test
	public void scorpio_lower_bound_inclusive() {
		int longitude = 210;
		
		Zodiac zodiac = this.zodiacService.findByLongitude(longitude);
		
		assertThat(zodiac, not(nullValue()));
		assertThat(zodiac.getName(), equalTo("Scorpio"));
	}

	/** <P>Scorpio runs from 210 to 239.
	 * </P>
	 */
	@Test
	public void scorpio_mid_range() {
		int longitude = 239;
		
		Zodiac zodiac = this.zodiacService.findByLongitude(longitude);
		
		assertThat(zodiac, not(nullValue()));
		assertThat(zodiac.getName(), equalTo("Scorpio"));
	}

	/** <P>Scorpio runs from 210 to 239.
	 * </P>
	 */
	@Test
	public void scorpio_upper_bound_exclusive() {
		int longitude = 240;
		
		Zodiac zodiac = this.zodiacService.findByLongitude(longitude);
		
		assertThat(zodiac, not(nullValue()));
		assertThat(zodiac.getName(), not(equalTo("Scorpio")));
		assertThat(zodiac.getName(), equalTo("Sagittarius"));
	}

}
