package com.sayarti.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sayarti.backend.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIndexesIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayAppliesDatabaseIndexMigration() {
        Map<String, Object> migration = jdbc.queryForMap("""
                SELECT version, description, success
                FROM flyway_schema_history
                WHERE version = '15'
                """);

        assertThat(migration.get("version").toString()).isEqualTo("15");
        assertThat(migration.get("description")).isEqualTo("add v1 query pattern indexes");
        assertThat(migration.get("success")).isEqualTo(true);
    }

    @Test
    void queryPatternIndexesHaveExpectedKeyAndIncludedColumns() {
        assertIndex("vehicles", "ix_vehicles_user_active_created",
                List.of("user_id", "deleted_at", "created_at"), List.of());
        assertIndex("fuel_records", "ix_fuel_records_vehicle_active_filled",
                List.of("vehicle_id", "deleted_at", "filled_at", "created_at", "id"),
                List.of());
        assertIndex("maintenance_records", "ix_maintenance_records_vehicle_active_service",
                List.of("vehicle_id", "deleted_at", "service_date", "created_at", "id"),
                List.of());
        assertIndex("reminders", "ix_reminders_vehicle_active_incomplete",
                List.of("vehicle_id", "deleted_at", "completed", "created_at", "id"),
                List.of());
        assertIndex("reminders", "ix_reminders_vehicle_active_completed",
                List.of("vehicle_id", "deleted_at", "completed", "completed_at", "id"),
                List.of());
        assertIndex("reminders", "ix_reminders_pending_date_trigger",
                List.of("target_date", "created_at", "id"), List.of("vehicle_id"));
        assertIndex("reminders", "ix_reminders_pending_mileage_trigger",
                List.of("target_mileage", "created_at", "id"), List.of("vehicle_id"));
    }

    @Test
    void schedulerIndexesAreFilteredToPendingTriggerRows() {
        assertFilter("ix_reminders_pending_date_trigger", "[notification_delivered_at] IS NULL",
                "[completed]=(0)", "[deleted_at] IS NULL", "[trigger_type]='DATE'");
        assertFilter("ix_reminders_pending_mileage_trigger",
                "[notification_delivered_at] IS NULL", "[completed]=(0)",
                "[deleted_at] IS NULL", "[trigger_type]='MILEAGE'");
    }

    @Test
    void existingRequiredUniqueIndexesRemainPresent() {
        assertUniqueIndex("users", "uq_users_email");
        assertUniqueIndex("users", "uq_users_google_subject");
        assertUniqueIndex("refresh_tokens", "uq_refresh_tokens_hash");
        assertUniqueIndex("email_verification_otps", "uq_email_verification_otps_active_user");
        assertUniqueIndex("devices", "uq_devices_user_identifier");
        assertUniqueIndex("devices", "uq_devices_fcm_token");
    }

    private void assertIndex(String table, String index, List<String> keys,
            List<String> included) {
        List<Map<String, Object>> columns = jdbc.queryForList("""
                SELECT c.name AS column_name, ic.key_ordinal, ic.is_included_column
                FROM sys.indexes i
                JOIN sys.tables t ON t.object_id = i.object_id
                JOIN sys.index_columns ic
                  ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                JOIN sys.columns c
                  ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                WHERE t.name = ? AND i.name = ?
                ORDER BY ic.is_included_column, ic.key_ordinal, ic.index_column_id
                """, table, index);

        assertThat(columns.stream().filter(row -> !asBoolean(row.get("is_included_column")))
                .map(row -> row.get("column_name").toString()).toList()).isEqualTo(keys);
        assertThat(columns.stream().filter(row -> asBoolean(row.get("is_included_column")))
                .map(row -> row.get("column_name").toString()).toList()).isEqualTo(included);
    }

    private void assertFilter(String index, String... fragments) {
        String filter = jdbc.queryForObject(
                "SELECT filter_definition FROM sys.indexes WHERE name = ?", String.class, index);
        assertThat(filter).contains(fragments);
    }

    private void assertUniqueIndex(String table, String index) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.indexes i
                JOIN sys.tables t ON t.object_id = i.object_id
                WHERE t.name = ? AND i.name = ? AND i.is_unique = 1
                """, Integer.class, table, index);
        assertThat(count).isEqualTo(1);
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue
                ? booleanValue
                : ((Number) value).intValue() != 0;
    }
}
