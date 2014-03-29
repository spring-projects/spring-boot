package sample.data.gemfire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The GemstoneAppConfiguration class for allowing Spring Boot to pickup additional application Spring configuration
 * meta-data for GemFire, which must be specified in Spring Data GemFire's XML namespace.
 * <p/>
 * @author John Blum
 * @see org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * @see org.springframework.context.annotation.ComponentScan
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.ImportResource
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.springframework.transaction.annotation.EnableTransactionManagement
 * @since 1.0.0
 */
@Configuration
@ImportResource("/spring-data-gemfire-cache.xml")
@ComponentScan
@EnableAutoConfiguration
@EnableGemfireRepositories
@EnableTransactionManagement
@SuppressWarnings("unused")
public class SampleDataGemFireApplication {

  public static void main(final String[] args) {
    SpringApplication.run(SampleDataGemFireApplication.class, args);
  }

}
