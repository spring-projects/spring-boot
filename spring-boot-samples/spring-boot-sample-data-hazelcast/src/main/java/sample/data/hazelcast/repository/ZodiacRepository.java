package sample.data.hazelcast.repository;

import org.springframework.data.hazelcast.repository.HazelcastRepository;

import sample.data.hazelcast.domain.Zodiac;

/**
 * <P>A repository object to access the Zodiac domain objects.
 * </P>
 * <P>Extend this with a method to find "{@code longitude <= x < longitude}",
 * Spring will deduce the implementation.
 * </P>
 * 
 * @author Neil Stevenson
 */
public interface ZodiacRepository extends HazelcastRepository<Zodiac, Integer> {
	
	public Zodiac findByLongitudeGreaterThanAndLongitudeLessThanEqual(int lower, int upper);
}
