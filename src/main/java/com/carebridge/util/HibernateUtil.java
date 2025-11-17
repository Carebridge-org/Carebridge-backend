package com.carebridge.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import io.github.cdimascio.dotenv.Dotenv;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Dotenv dotenv = Dotenv.load();
            String dbUrl = dotenv.get("DATABASE_URL");

            // Parse JDBC URL til base URL + query parameters
            java.net.URI uri = new java.net.URI(dbUrl.replace("jdbc:postgresql://", "postgresql://"));
            String user = null;
            String password = null;

            // Parse query params
            String[] queryParts = uri.getQuery().split("&");
            StringBuilder baseUrl = new StringBuilder("jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
            for (String part : queryParts) {
                if (part.startsWith("user=")) user = part.split("=")[1];
                else if (part.startsWith("password=")) password = part.split("=")[1];
            }

            Configuration config = new Configuration();
            config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            config.setProperty("hibernate.connection.url", baseUrl.toString());
            config.setProperty("hibernate.connection.username", user);
            config.setProperty("hibernate.connection.password", password);
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            config.setProperty("hibernate.hbm2ddl.auto", "update");
            config.setProperty("hibernate.show_sql", "true");

            config.addAnnotatedClass(com.carebridge.models.User.class);
            config.addAnnotatedClass(com.carebridge.models.LoginAttempt.class);

            return config.buildSessionFactory();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }


    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
