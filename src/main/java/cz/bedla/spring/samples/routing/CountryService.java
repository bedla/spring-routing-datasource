package cz.bedla.spring.samples.routing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CountryService {
    private final JdbcTemplate jdbcTemplate;

    CountryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void select() {
        System.out.println("        CountryService.select() -> currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("        " + TransactionSynchronizationManager.getResourceMap());
        var list = jdbcTemplate.queryForList("SELECT * FROM country");
        System.out.println("        " + list);
    }

    @Transactional
    public void select(Runnable callAfter) {
        select();
        callAfter.run();
    }
}
