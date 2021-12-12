package cz.bedla.spring.samples.routing;

import org.springframework.transaction.annotation.Transactional;

public class DataSourceInDataSourceCaller {
    private final CountryService countryService;
    private final PersonService personService;

    DataSourceInDataSourceCaller(CountryService countryService, PersonService personService) {
        this.countryService = countryService;
        this.personService = personService;
    }

    @Transactional
    public void callPersonThenCountry() {
        personService.select(() -> CorrectRoutingDataSource.runWith(DataSourceType.COUNTRY, () -> countryService.select()));
    }

    @Transactional
    public void callCountryThenPerson() {
        countryService.select(() -> CorrectRoutingDataSource.runWith(DataSourceType.PERSON, () -> personService.select()));
    }
}
