package CRM_Manara.CRM_Manara.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;

@Component
@Order(1)
public class AuthSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public AuthSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // ADDED
        if (!columnExists("users", "enabled")) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");
        }

        // ADDED
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS verification_tokens (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    token VARCHAR(255) NOT NULL,
                    user_id BIGINT NOT NULL,
                    expiration_date DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_verification_tokens_token (token),
                    UNIQUE KEY uk_verification_tokens_user_id (user_id),
                    CONSTRAINT fk_verification_tokens_user
                        FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parent_notifications (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    parent_id BIGINT NOT NULL,
                    category VARCHAR(80) NOT NULL,
                    title VARCHAR(160) NOT NULL,
                    message VARCHAR(1200) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    read_status BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (id),
                    KEY idx_parent_notifications_parent_id (parent_id),
                    CONSTRAINT fk_parent_notifications_parent
                        FOREIGN KEY (parent_id) REFERENCES parent(id)
                )
                """);

        ensureInscriptionStatuses();
    }

    private void ensureInscriptionStatuses() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("mariadb") || product.contains("mysql")) {
                jdbcTemplate.execute("""
                        ALTER TABLE inscription
                        MODIFY COLUMN status_inscription ENUM('EN_ATTENTE','APPROUVEE','REFUSEE','ACTIF','ANNULÉE') NULL
                        """);
            }
        } catch (Exception ignored) {
        }
    }

    // ADDED
    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     connection.getCatalog(),
                     null,
                     tableName,
                     columnName
             )) {
            if (columns.next()) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     connection.getCatalog(),
                     null,
                     tableName.toUpperCase(),
                     columnName.toUpperCase()
             )) {
            return columns.next();
        } catch (Exception ignored) {
            return false;
        }
    }
}
