package cz.bedla.spring.samples.routing;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class CorrectDatabaseConfig extends AbstractDatabaseConfig {
    @Override
    protected MyAbstractRoutingDataSource createRoutingDataSource() {
        return new CorrectRoutingDataSource();
    }
}
