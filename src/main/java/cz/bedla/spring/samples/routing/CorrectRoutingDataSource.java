package cz.bedla.spring.samples.routing;

import org.springframework.core.InfrastructureProxy;

public class CorrectRoutingDataSource extends MyAbstractRoutingDataSource implements InfrastructureProxy {
    @Override
    public Object getWrappedObject() {
        return determineTargetDataSource();
    }
}
