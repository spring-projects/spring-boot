package org.test

@Grab("org.hsqldb:hsqldb:2.2.9")

@Configuration
@EnableTransactionManagement
class Example implements CommandLineRunner {

  @Autowired
  JdbcTemplate jdbcTemplate

  @Transactional
  void run(String... args) {
    println "Foo count=" + jdbcTemplate.queryForObject("SELECT COUNT(*) from FOO", Integer)
  }

}

