package cz.bedla.spring.samples.routing;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class Starter {
    private final PersonService personService;
    private final CountryService countryService;
    private final DataSourceInDataSourceCaller innerCaller;

    Starter(PersonService personService, CountryService countryService, DataSourceInDataSourceCaller innerCaller) {
        this.personService = personService;
        this.countryService = countryService;
        this.innerCaller = innerCaller;
    }

    public void startParallel() {
        System.out.println("begin Starter.startParallel()");
        System.out.println("    Starter.startParallel() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        CorrectRoutingDataSource.runWith(DataSourceType.PERSON, () -> personService.select());
        System.out.println("    Starter.startParallel() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("    ***");
        System.out.println("    Starter.startParallel() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        CorrectRoutingDataSource.runWith(DataSourceType.COUNTRY, () -> countryService.select());
        System.out.println("    Starter.startParallel() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("end Starter.startParallel()");
    }

    public void startInner(boolean innerShouldFail) {
        System.out.println("begin Starter.startInner()");
        System.out.println("    Starter.startInner() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        if (innerShouldFail) {
            try {
                CorrectRoutingDataSource.runWith(DataSourceType.PERSON, () -> innerCaller.callPersonThenCountry());
            } catch (Exception e) {
                System.out.println("        " + e);
            }
        } else {
            CorrectRoutingDataSource.runWith(DataSourceType.PERSON, () -> innerCaller.callPersonThenCountry());
        }
        System.out.println("    Starter.startInner() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("    ***");
        System.out.println("    Starter.startInner() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        if (innerShouldFail) {
            try {
                CorrectRoutingDataSource.runWith(DataSourceType.COUNTRY, () -> innerCaller.callCountryThenPerson());
            } catch (Exception e) {
                System.out.println("        " + e);
            }
        } else {
            CorrectRoutingDataSource.runWith(DataSourceType.COUNTRY, () -> innerCaller.callCountryThenPerson());
        }
        System.out.println("    Starter.startInner() currentTransactionName = " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("end Starter.startInner()");
    }

    @Transactional
    public void startInsideTransaction() {
        throw new IllegalStateException("This has to fail on unable to determine DataSource lookup key exception");
    }

    public void wrongTableInPersonDB() {
        System.out.println("begin Starter.wrongTableInPersonDB()");
        try {
            CorrectRoutingDataSource.runWith(DataSourceType.PERSON, () -> countryService.select());
        } catch (Exception e) {
            System.out.println("            " + e);
        }
        System.out.println("end Starter.wrongTableInPersonDB()");
    }

    public void wrongTableInCountryDB() {
        System.out.println("begin Starter.wrongTableInCountryDB()");
        try {
            CorrectRoutingDataSource.runWith(DataSourceType.COUNTRY, () -> personService.select());
        } catch (Exception e) {
            System.out.println("            " + e);
        }
        System.out.println("end Starter.wrongTableInCountryDB()");
    }
}
