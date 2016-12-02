package sample.data.hazelcast.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hazelcast.HazelcastKeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueTemplate;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <P>Spring configuration to use {@link www.hazelcast.org Hazelcast} as
 * the storage layer for Key-Value repositories.
 * </P>
 * <P>Hazelcast provides a storage structure called an {@link com.hazelcast.core.IMap IMap}
 * that extends {@link java.util.Map}. So, you can do {@code put(key,value)} and {@code get(key)}
 * and all the other map operations without knowing about Hazelcast.
 * </P>
 * <P>Hazelcast is normally set-up so that the "<I>server</I>" storage process forms a cluster
 * with other such Hazelcast processes, and automatically stripes the map content across the
 * group. Eg. with 5 processes each might hold 1/5th of the content. This is abstracted away
 * from the caller, so you can keep doing {@code put(key,value)} and {@code get(key)} without
 * knowing if the data is actually stored on the current process or one of the others in the
 * group. You can then store more in the group than any one process can cope with.
 * <P>
 * <P>To make testing simple, networking is disabled so that process can't find any others,
 * and so forms a cluster of 1.
 * </P>
 * <P><B>PLUS</B>
 * </P>We then wrap everything up as a Spring Data {@code @Repository}, so then we can
 * do CRUD and query operations, further hiding that it's Hazelcast underneath, and
 * making everything be usable following standard Spring mechanisms such as
 * {@code save(value)} and {@code findOne(key)}.
 * </P>
 * 
 * @author Neil Stevenson
 */
@Configuration
public class ZodiacConfig {
	
	/**
	 * <P>Create a Hazelcast server for storing the Zodiac data, rather than
	 * let Spring Boot auto-configure do so.
	 * </P>
	 * <P>Turn off default network, and don't turn on any other kinds. Without
	 * networking, this instance will be standalone.
	 * </P>
	 * 
	 * @return A standalone Hazelcast server
	 */
    @Bean
    public HazelcastInstance hazelcastInstance() {
            Config config = new Config();
            
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            
            return Hazelcast.newHazelcastInstance(config);
    }
	
	/**
	 * <P>Use Hazelcast as the implementation class for Key-Value operations.
	 * </P>
	 * 
	 * @param hazelcastInstance Created above
	 * @return Template for Key-Value operations
	 */
    @Bean
    public KeyValueTemplate keyValueTemplate(HazelcastInstance hazelcastInstance) {
            return new KeyValueTemplate(new HazelcastKeyValueAdapter(hazelcastInstance));
    }

}
