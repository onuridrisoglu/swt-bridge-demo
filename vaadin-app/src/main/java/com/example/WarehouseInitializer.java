package com.example;

import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.seed.DataSeeder;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Opens and seeds the shared H2 database at startup, for the host's own
 * views (login, modern dashboard). Each user's SWT application, in the same
 * JVM, opens its own connections to the same database, the file
 * {@code -Dwarehouse.db} names (H2 {@code AUTO_SERVER}).
 */
@Component
public class WarehouseInitializer {

    @PostConstruct
    void init() {
        PersistenceUtil.init();
        DataSeeder.seedIfEmpty();
    }
}
