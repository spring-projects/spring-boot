package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.Mongo;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;

@Configuration
@ConditionalOnClass({ Mongo.class, MongodStarter.class})
public class EmbedMongoAutoConfiguration {

	@Autowired
	private MongoProperties properties;

	@Bean(initMethod = "start", destroyMethod = "stop")
	public MongodExecutable embedMongoServer() throws IOException {
		IMongodConfig mongodConfig = new MongodConfigBuilder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(properties.getPort(), localhostIsIPv6()))
				.build();
		return MongodStarter.getDefaultInstance().prepare(mongodConfig);
	}

}
