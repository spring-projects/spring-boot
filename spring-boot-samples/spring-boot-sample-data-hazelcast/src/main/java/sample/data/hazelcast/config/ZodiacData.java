package sample.data.hazelcast.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import sample.data.hazelcast.domain.Zodiac;
import sample.data.hazelcast.repository.ZodiacRepository;

/**
 * <P>Load the Zodiac data. Use a {@link CommandLineRunner} with a low {@link Order}
 * to ensure data loaded before we might need to use it.
 * </P>
 * 
 * @author Neil Stevenson
 *
 * @see <A HREF="https://en.wikipedia.org/wiki/Astrological_sign">Astrological Sign</A>
 */
@Order(value=0)
@Component
public class ZodiacData implements CommandLineRunner {
	
	@Autowired
	private ZodiacRepository zodiacRepository;

	/**
	 * <P>Inject data via a {@code @Repository} rather than directly into
	 * a Hazelcast {@link com.hazelcast.core.IMap IMap}.
	 * 
	 * @param Not used
	 */
	@Override
	public void run(String... args) throws Exception {
		
		for (int i=0 ; i < data.length ; i++) {
			Zodiac zodiac = new Zodiac();
			
			zodiac.setId(i);
			zodiac.setDescription(data[i][1].toString());
			zodiac.setLongitude(i*30);
			zodiac.setName(data[i][0].toString());
			
			this.zodiacRepository.save(zodiac);
		}
		
	}
	
	private Object[][] data = new Object[][] {
		{ "Aries", 	"The Ram"},
		{ "Taurus", "The Bull"},
		{ "Gemini", "The Twins"},
		{ "Cancer", "The Crab"},
		{ "Leo", "The Lion"},
		{ "Virgo", "The Maiden"},
		{ "Libra", "The Scales"},
		{ "Scorpio", "The Scorpion"},
		{ "Sagittarius", "The Archer"},
		{ "Capricorn", "The Mountain Sea-goat"},
		{ "Aquarius", "The Water-bearer"},
		{ "Pisces", "The Fish"},
	};
	
}
