# Usage of Spring's `AbstractRoutingDataSource` with nested calls to different data sources

This repository contains example and steps-to-reproduce how is use Spring's `AbstractRoutingDataSource` that can switch
between data sources based on some key. This can be used when we have multiple data sources that can be used to connect
do different databases.

First part shows problems with naive implementation of child class of this `AbstractRoutingDataSource`. Second part
shows how those problems could be resolved.

Both parts reuse implementation of class `MyAbstractRoutingDataSource` that shows how both data sources are
distinguished from each other.

It contains definition of two data sources `AbstractDatabaseConfig.dataSourcePerson(...)` and
`AbstractDatabaseConfig.dataSourceCountry(...)`. Those databases contain two different tables to distinguish between
states when SQL `SELECT` fails on missing table.

## Failing implementation of `AbstractRoutingDataSource`

Failing implementation just implements abstract `determineCurrentLookupKey()` method and select correct key of data
source from `ThreadLocal<DataSourceType> key` static field.

First Spring context starts and creates two data sources and one routing data source.

```
Dec 10, 2021 8:03:27 PM org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory initDatabase
INFO: Starting embedded database: url='jdbc:h2:mem:databasePerson-1c467d0e-4960-44a4-a6cd-4aafc560b820;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
Dec 10, 2021 8:03:27 PM org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory initDatabase
INFO: Starting embedded database: url='jdbc:h2:mem:databaseCountry-a7d645c8-061e-4d89-baf1-bd2d19ae80dc;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
dataSourcePerson = org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy@15c25153
dataSourceCountry = org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy@56276db8
```

Mind that identity hash code of those two data sources is:

- for `dataSourcePerson` it equals value `@15c25153`
- for `dataSourceCountry` it equals value `@56276db8`

### 1. Use-case - parallel transaction

Method `Starter.startParallel()` runs without any transaction. This means that no data source is routed.

```
    Starter.startParallel() currentTransactionName = null
        PersonService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.PersonService.select
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@773f7880}
        [{NAME=Ivo}, {NAME=Kvetinac}, {NAME=Vopicak}]
    Starter.startParallel() currentTransactionName = null
```

As you can see we `SELECT` data from Person data source, and it returns correct data. Mind
that `TransactionSynchronizationManager.resourceMap` contains one key that points to
`FailingRoutingDataSource` with identity hash code `@65a4798f`. It means that transaction synchronization does not
distinguish between those two data source that are used to query correct data, but it contains key to
main `FailingRoutingDataSource` instance. This will be problem, see use cases below. Also mind that current transaction
starts, queries data from correct data source and then finishes.

Second query to Country data source is also correct, except of keys in `TransactionSynchronizationManager.resourceMap`.

```
    Starter.startParallel() currentTransactionName = null
        CountryService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.CountryService.select
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@426b6a74}
        [{NAME=Czech Republic}, {NAME=Japan}, {NAME=US & A}]
    Starter.startParallel() currentTransactionName = null
```

Resources map contains only one key to `FailingRoutingDataSource` with identity hash code `@65a4798f`. Also mind that
current transaction starts, queries data from correct data source and then finishes.

### 2. Use-case - inner transaction

Method `Starter.startInner()` runs without any transaction. This means that no data source is routed.

First variant of use case opens transaction and queries **Person data source**. In this case transaction is bound
to `FailingRoutingDataSource` with identity hash code `@65a4798f` and
`ConnectionHolder` with identity hash code `@665e9289` contains `Connection` instance that points to Person data source.

Next step in same inner/nested transaction is to query in another transaction **Country data source**. As you can see
from exception it is not possible to find `country` table in transaction bound to
`FailingRoutingDataSource` with identity hash code `@65a4798f` because it already contains `ConnectionHolder` with
identity hash code `@665e9289` with `Connection` instance that points to **Person data source**. This state results
to `BadSqlGrammarException` with message `Table "COUNTRY" not found`.

```
    Starter.startInner() currentTransactionName = null
        PersonService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.DataSourceInDataSourceCaller.callPersonThenCountry
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@665e9289}
        [{NAME=Ivo}, {NAME=Kvetinac}, {NAME=Vopicak}]
        CountryService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.DataSourceInDataSourceCaller.callPersonThenCountry
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@665e9289}
        org.springframework.jdbc.BadSqlGrammarException: StatementCallback; bad SQL grammar [SELECT * FROM country]; nested exception is org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "COUNTRY" not found; SQL statement: SELECT * FROM country [42102-202]
    Starter.startInner() currentTransactionName = null
```

Similar situation is when we swap calls and first query **Country data source** and then *Person data source*.

```
    Starter.startInner() currentTransactionName = null
        CountryService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.DataSourceInDataSourceCaller.callCountryThenPerson
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@2756c0a7}
        [{NAME=Czech Republic}, {NAME=Japan}, {NAME=US & A}]
        PersonService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.DataSourceInDataSourceCaller.callCountryThenPerson
        {cz.bedla.spring.samples.routing.FailingRoutingDataSource@65a4798f=org.springframework.jdbc.datasource.ConnectionHolder@2756c0a7}
        org.springframework.jdbc.BadSqlGrammarException: StatementCallback; bad SQL grammar [SELECT * FROM person]; nested exception is org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "PERSON" not found; SQL statement: SELECT * FROM person [42102-202]
    Starter.startInner() currentTransactionName = null
```

## Correct implementation of `AbstractRoutingDataSource`

Problem with previous implementation of `AbstractRoutingDataSource` is that when it bounds data source, and it's
connection to current thread it uses instance of data source itself but not data source that is currently selected.

As you can see from previous fail statuses **it is not possible open inner/nested transaction inside already created
transaction**. One can expect that when we select another data source using key transaction-manager will switch to
another transaction.

As you can see in `CorrectRoutingDataSource` we can implement `org.springframework.core.InfrastructureProxy` that as
it's JavaDoc suggests is used to _"Interface to be implemented by transparent resource proxies that need to be
considered as equal to the underlying resource, for example for consistent lookup key comparisons."_

When we implement this interface's method `getWrappedObject` we can say what has to be used as key by
`TransactionSynchronizationManager.resourceMap`.

```java
    @Override
    public Object getWrappedObject(){
        return determineTargetDataSource();
    }
```

From now each data source has its own transaction bound to thread with correct keys and connection holder.

We can see it from `startInner()` run.

```
    Starter.startInner() currentTransactionName = null
        PersonService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.DataSourceInDataSourceCaller.callPersonThenCountry
        {org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy@602e0143=org.springframework.jdbc.datasource.ConnectionHolder@6b98a075}
        [{NAME=Ivo}, {NAME=Kvetinac}, {NAME=Vopicak}]
        CountryService.select() -> currentTransactionName = cz.bedla.spring.samples.routing.CountryService.select
        {org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy@32c726ee=org.springframework.jdbc.datasource.ConnectionHolder@e84a8e1, org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy@602e0143=org.springframework.jdbc.datasource.ConnectionHolder@6b98a075}
        [{NAME=Czech Republic}, {NAME=Japan}, {NAME=US & A}]
    Starter.startInner() currentTransactionName = null
```

Invocation of inner/nested transaction per data source looks like this:

1. We query for content of `person` table inside `DataSourceInDataSourceCaller.callPersonThenCountry` transaction.
   `TransactionSynchronizationManager.resourceMap` contains only **one key** that points to **Person data source** with
   identity hash code `@602e0143`.
2. When we open new inner/nested transaction `CountryService.select` and queries data from Country data source.
   Now `TransactionSynchronizationManager.resourceMap` contains **two keys**, first points to **Person data source**
   with identity hash code `@602e0143`, and second points to **Country data** source with identity hash code
   `@32c726ee`.

This is opposite to previous implementation when we had only on key that was pointing to implementation of Routing data
source not to any child data source.

## What is the problem?

Question here is why `AbstractRoutingDataSource` does not implement `InfrastructureProxy` interface to correctly
distinguish between data sources and to select correct key for `TransactionSynchronizationManager.resourceMap` that
holds thread and key bound `ConnectionHolder`.

Are there any edge cases with other JDBC frameworks like Hibernate or other overcomplicated/complex data access
libraries?

Can be this usage of `InfrastructureProxy` interface considered as abuse of interface?

Is this gonna correctly work only with pure JDBC access?
