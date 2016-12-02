package sample.data.hazelcast.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.hazelcast.repository.config.EnableHazelcastRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import com.hazelcast.core.HazelcastInstance;

import sample.data.hazelcast.config.ZodiacConfig;
import sample.data.hazelcast.config.ZodiacData;
import sample.data.hazelcast.domain.Zodiac;

/**
 * <P>Test the Hazelcast repository.
 * </P>
 * 
 * @author Neil Stevenson
 */
@EnableHazelcastRepositories
@RunWith(SpringRunner.class)
@SpringBootTest(classes={ZodiacConfig.class, ZodiacData.class})
public class ZodiacRepositoryTests {
	
	@Autowired
	private HazelcastInstance hazelcastInstance;
	@Autowired
	private ZodiacRepository zodiacRepository;
	
	/**
	 * <P>Test that Hazelcast has an {@link com.hazelcast.core.IMap IMap}
	 * for the {@link Zodiac} data.
	 * </P>
	 */
	@Test
	public void imap_exists_and_not_empty() {
		String mapName = Zodiac.class.getCanonicalName();
		
		Map<?, ?> map = (Map<?, ?>) this.hazelcastInstance.getMap(mapName);
		
		assertThat(map, not(nullValue()));
		assertThat(map.size(), equalTo(12));
	}
	
	/**
	 * <P>Test a value that isn't first or last.
	 * </P>
	 */
	@Test
	public void scorpio() {
		Zodiac zodiac = this.zodiacRepository.findOne(7);
		
		assertThat(zodiac, not(nullValue()));
		assertThat("Id", zodiac.getId(), equalTo(7));
		assertThat("Longitude", zodiac.getLongitude(), equalTo(210));
		assertThat("Name", zodiac.getName(), equalTo("Scorpio"));
		assertThat("Description", zodiac.getDescription(), equalTo("The Scorpion"));
	}
	
	/**
	 * <P>Test all star signs present exactly once.
	 * </P>
	 */
	@Test
	public void all_signs() {
		Set<Integer> keys = new TreeSet<>();

		Iterator<Zodiac> iterator = this.zodiacRepository.findAll().iterator();
		
		while (iterator.hasNext()) {
			keys.add(iterator.next().getId());
		}
		
		assertThat("12 unique", keys.size(), equalTo(12));
		assertThat("12 in total", this.zodiacRepository.count(), equalTo(12L));
	}
}
