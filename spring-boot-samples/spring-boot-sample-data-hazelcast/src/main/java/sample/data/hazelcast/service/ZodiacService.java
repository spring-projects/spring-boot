package sample.data.hazelcast.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sample.data.hazelcast.domain.Zodiac;
import sample.data.hazelcast.repository.ZodiacRepository;

/**
 * <P>A Spring {@code @Service} object, to avoid exposing the {@code @Repository}
 * to any calling modules.
 * </P>
 * 
 * @author Neil Stevenson
 */
@Service
public class ZodiacService {

	@Autowired
	private ZodiacRepository zodiacRepository;

	/**
	 * <P>Take the provided repository and use a query to find the Zodiac sign,
	 * exploiting the fact that each sign has exact 30 of 360 degrees.
	 * </P>
	 * 
	 * @param longitude Assumed to be in range 0-359
	 * @return One sign of the Zodiac
	 */
	public Zodiac findByLongitude(final int longitude) {
		int lower = longitude - 30;
		int upper = longitude;
		return this.zodiacRepository.findByLongitudeGreaterThanAndLongitudeLessThanEqual(lower, upper);
	}
	
}
