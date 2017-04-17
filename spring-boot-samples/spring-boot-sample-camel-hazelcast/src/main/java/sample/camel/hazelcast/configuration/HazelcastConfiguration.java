package sample.camel.hazelcast.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.camel.component.hazelcast.HazelcastComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.Collections;

@Configuration(value = "hazelcastConfiguration")
public class HazelcastConfiguration {

	@Value("${hazelcast.group.name:test}")
	private String hazelcastGroupName;

	@Value("${hazelcast.group.password:test}")
	private String hazelcastGroupPasword;

	@Value("${hazelcast.interface.enable:true}")
	private boolean hazelcastInterfaceEnable;

	@Value("${hazelcast.interfaces:*.*.*.*}")
	private String hazelcastInterfaces;

	@Value("${hazelcast.multicast.enable:true}")
	private boolean hazelcastMulticastEnable;

	@Value("${hazelcast.multicast.group:224.2.2.5}")
	private String hazelcastMulticastGroup;


	@SuppressWarnings("unused")
	@Bean
	public HazelcastComponent hazelcast() throws UnknownHostException {
		final HazelcastComponent hazelcastComponent = new HazelcastComponent();
		hazelcastComponent.setHazelcastInstance(this.createHazelcastInstance());
		return hazelcastComponent;
	}

	private HazelcastInstance createHazelcastInstance() {
		Config config = new Config();
		config.getGroupConfig().setName(hazelcastGroupName).setPassword(hazelcastGroupPasword);

		NetworkConfig network = config.getNetworkConfig();
		network.getInterfaces().setEnabled(hazelcastInterfaceEnable);
		network.getInterfaces().setInterfaces(Collections.singleton(hazelcastInterfaces));
		network.getJoin().getMulticastConfig().setEnabled(hazelcastMulticastEnable);
		network.getJoin().getMulticastConfig().setMulticastGroup(hazelcastMulticastGroup);
		return Hazelcast.newHazelcastInstance(config);
	}
}

