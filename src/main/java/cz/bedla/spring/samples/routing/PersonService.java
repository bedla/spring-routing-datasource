package cz.bedla.spring.samples.routing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PersonService {
    private final JdbcTemplate jdbcTemplate;

    PersonService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void select() {
        System.out.println("        PersonService.select() -> currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("        " + TransactionSynchronizationManager.getResourceMap());
        var list = jdbcTemplate.queryForList("SELECT * FROM person");
        System.out.println("        " + list);
    }

    @Transactional
    public void select(Runnable callAfter) {
        select();
        callAfter.run();
    }
}
