import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.MongoRepositoryConfigurationExtension;
import org.springframework.data.mongodb.repository.config.MongoRepositoryConfigurationSupport;

@Configuration
@Import(SslAutoConfiguration.class) // Add this line to import SslAutoConfiguration
@EnableMongoRepositories(repositoryBaseClass = MongoRepositoryConfigurationSupport.class)
public class MyMongoConfiguration {
    // Configuration code goes here
}
