package sample.data.hazelcast.domain;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import lombok.Data;

/**
 * <P>A domain object represent a sign of the Zodiac.
 * </P>
 * 
 * @author Neil Stevenson
 * 
 * @see <A HREF="https://en.wikipedia.org/wiki/Astrological_sign">Astrological Sign</A>
 */
@SuppressWarnings("serial")
@Data
@KeySpace
public class Zodiac implements Serializable {

	@Id
	private int 	id;
	
	private String	name;
	private String  description;
	private int     longitude;
	
}
