package com.example.warehouse.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;

public class PersistenceUtil {

    /**
     * Where the H2 database lives, without the {@code .mv.db} suffix; default
     * {@code ./warehousedb} in the working directory. Set it to share one
     * database between processes, e.g. the Vaadin host and the bridged SWT
     * application, which runs in a working directory of its own.
     */
    public static final String DB_PROPERTY = "warehouse.db";

    private static EntityManagerFactory emf;

    public static void init() {
        if (emf == null) {
            String db = System.getProperty(DB_PROPERTY);
            emf = db == null || db.isBlank()
                    ? Persistence.createEntityManagerFactory("warehouseDB")
                    : Persistence.createEntityManagerFactory("warehouseDB", Map.of(
                            "jakarta.persistence.jdbc.url",
                            "jdbc:h2:" + db.trim() + ";AUTO_SERVER=TRUE"));
        }
    }

    public static EntityManager getEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("PersistenceUtil not initialized. Call init() first.");
        }
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
