package cz.bedla.spring.samples.routing;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;

public class Main2Databases1RoutingDatasource {
    public static void main(String[] args) {
        // for H2 error messages
        Locale.setDefault(Locale.ENGLISH);

        runApplicationContext(CorrectDatabaseConfig.class);
        System.out.println();
        System.out.println("=========================");
        System.out.println();
        runApplicationContext(FailingDatabaseConfig.class);
    }

    private static void runApplicationContext(Class<?> configClass) {
        try (var context = new AnnotationConfigApplicationContext(configClass)) {
            context.start();

            var starter = context.getBean(Starter.class);
            starter.startParallel();
            System.out.println("------------------");
            starter.startInner(configClass == FailingDatabaseConfig.class);
            System.out.println("------------------");
            transactionWithoutDataSourceKeySpecified(starter);
            System.out.println("------------------");
            starter.wrongTableInPersonDB();
            System.out.println("------------------");
            starter.wrongTableInCountryDB();
        }
    }

    private static void transactionWithoutDataSourceKeySpecified(Starter starter) {
        try {
            System.out.println("begin Starter.startInsideTransaction()");
            starter.startInsideTransaction();
        } catch (Exception e) {
            System.out.println("    " + e);
        } finally {
            System.out.println("end Starter.startInsideTransaction()");
        }
    }
}

