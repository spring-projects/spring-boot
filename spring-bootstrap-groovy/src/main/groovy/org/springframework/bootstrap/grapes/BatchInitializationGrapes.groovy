package org.springframework.bootstrap.grapes

@GrabResolver(name='spring-milestone', root='http://repo.springframework.org/milestone')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework:spring-jdbc:4.0.0.BOOTSTRAP-SNAPSHOT")
@Grab("org.springframework.batch:spring-batch-core:2.2.0.M1")
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import javax.annotation.PostConstruct
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

@Configuration // TODO: make this conditional
class BatchInitializationGrapes {

  @Autowired
  private DataSource dataSource

  @Autowired
  private Environment environment
  
  @Autowired
  private ResourceLoader resourceLoader
  
  @PostConstruct
  protected void initialize() {
    String platform = org.springframework.batch.support.DatabaseType.fromMetaData(dataSource).toString().toLowerCase()
    if (platform=="hsql") { 
      platform = "hsqldb"
    }
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator()
    populator.addScript(resourceLoader.getResource("org/springframework/batch/core/schema-${platform}.sql"))
    populator.setContinueOnError(true)
    DatabasePopulatorUtils.execute(populator, dataSource)
  }

}