package cz.bedla.spring.samples.routing;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractDatabaseConfig implements TransactionManagementConfigurer {
    @Bean
    Starter starter(PersonService personService, CountryService countryService, DataSourceInDataSourceCaller innerCaller) {
        return new Starter(personService, countryService, innerCaller);
    }

    @Bean
    PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    DataSource dataSource() {
        var dataSource = createRoutingDataSource();
        dataSource.setTargetDataSources(Map.of(
                DataSourceType.PERSON, dataSourcePerson(),
                DataSourceType.COUNTRY, dataSourceCountry()
        ));
        // setting null to default dataSource to fail when not correct key is specified and make it explicitly visible
        dataSource.setDefaultTargetDataSource(null);
        return dataSource;
    }

    protected abstract MyAbstractRoutingDataSource createRoutingDataSource();

    private DataSource dataSourcePerson() {
        final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("databasePerson-" + UUID.randomUUID())
                .addScript("classpath:/sql/schema-person.sql")
                .addScript("classpath:/sql/data-person.sql")
                .build();
        System.out.println("dataSourcePerson = " + database);
        return database;
    }

    private DataSource dataSourceCountry() {
        final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("databaseCountry-" + UUID.randomUUID())
                .addScript("classpath:/sql/schema-country.sql")
                .addScript("classpath:/sql/data-country.sql")
                .build();
        System.out.println("dataSourceCountry = " + database);
        return database;
    }

    @Bean
    JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    PersonService personService() {
        return new PersonService(jdbcTemplate());
    }

    @Bean
    CountryService countryService() {
        return new CountryService(jdbcTemplate());
    }

    @Bean
    DataSourceInDataSourceCaller innerCaller() {
        return new DataSourceInDataSourceCaller(countryService(), personService());
    }

    @Override
    public TransactionManager annotationDrivenTransactionManager() {
        return transactionManager();
    }
}
