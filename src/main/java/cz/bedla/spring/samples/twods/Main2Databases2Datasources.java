package cz.bedla.spring.samples.twods;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;

public class Main2Databases2Datasources {
    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            context.start();
            context.getBean(Starter.class).start();
        }
    }
}

@Configuration
@Import({
        PersonConfig.class,
        CountryConfig.class
})
class Config {
    @Bean
    Starter starter(PersonService personService, CountryService countryService) {
        return new Starter(personService, countryService);
    }
}

@Configuration
@EnableTransactionManagement
class PersonConfig implements TransactionManagementConfigurer {
    @Bean
    PlatformTransactionManager transactionManagerPerson() {
        return new DataSourceTransactionManager(dataSourcePerson());
    }

    @Bean
    DataSource dataSourcePerson() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("database1")
                .addScript("classpath:/sql/schema1.sql")
                .addScript("classpath:/sql/data1.sql")
                .build();
    }

    @Bean
    JdbcTemplate jdbcTemplatePerson() {
        return new JdbcTemplate(dataSourcePerson());
    }

    @Bean
    PersonService personService() {
        return new PersonService(jdbcTemplatePerson());
    }

    @Override
    public TransactionManager annotationDrivenTransactionManager() {
        return transactionManagerPerson();
    }
}

@Configuration
@EnableTransactionManagement
class CountryConfig {
    @Bean
    PlatformTransactionManager transactionManagerCountry() {
        return new DataSourceTransactionManager(dataSourceCountry());
    }

    @Bean
    DataSource dataSourceCountry() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("database2")
                .addScript("classpath:/sql/schema2.sql")
                .addScript("classpath:/sql/data2.sql")
                .build();
    }

    @Bean
    JdbcTemplate jdbcTemplateCountry() {
        return new JdbcTemplate(dataSourceCountry());
    }

    @Bean
    CountryService countryService() {
        return new CountryService(jdbcTemplateCountry());
    }
}

@Service
class PersonService {
    private final JdbcTemplate jdbcTemplate;

    PersonService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(transactionManager = "transactionManagerPerson")
    public void select() {
        System.out.println("currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println(TransactionSynchronizationManager.getResourceMap());
        var list = jdbcTemplate.queryForList("SELECT * FROM person");
        System.out.println(list);
    }
}

@Service
class CountryService {
    private final JdbcTemplate jdbcTemplate;

    CountryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(transactionManager = "transactionManagerCountry")
    public void select() {
        System.out.println("currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println(TransactionSynchronizationManager.getResourceMap());
        var list = jdbcTemplate.queryForList("SELECT * FROM country");
        System.out.println(list);
    }
}

class Starter {
    private final PersonService personService;
    private final CountryService countryService;

    Starter(PersonService personService, CountryService countryService) {
        this.personService = personService;
        this.countryService = countryService;
    }

    void start() {
        System.out.println("begin");
        System.out.println("currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        personService.select();
        System.out.println("***");
        countryService.select();
        System.out.println("end");
    }
}
