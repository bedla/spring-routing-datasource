package cz.bedla.spring.samples.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public abstract class MyAbstractRoutingDataSource extends AbstractRoutingDataSource {
    private static final ThreadLocal<DataSourceType> key = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return key.get();
    }

    public static void runWith(DataSourceType newKey, Runnable action) {
        var prevKey = key.get();
        key.set(newKey);
        try {
            action.run();
        } finally {
            key.set(prevKey);
        }
    }
}
