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
<<<<<<< HEAD
    public void run(String... args) throws Exception {
        String product = detectDbProduct();
        boolean isPostgres = product.contains("postgresql");
        boolean isMysql = product.contains("mariadb") || product.contains("mysql");

        // --- Migrations de colonnes (SQL standard, compatible tous SGBD) ---

=======
    public void run(String... args) {
        // ADDED
>>>>>>> origin/main
        if (!columnExists("users", "enabled")) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");
        }

        if (!columnExists("users", "avatar_url")) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN avatar_url VARCHAR(4096) NULL");
        }

<<<<<<< HEAD
=======
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
                    archived_status BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (id),
                    KEY idx_parent_notifications_parent_id (parent_id),
                    CONSTRAINT fk_parent_notifications_parent
                        FOREIGN KEY (parent_id) REFERENCES parent(id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS animateur_notifications (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    animateur_id BIGINT NOT NULL,
                    category VARCHAR(80) NOT NULL,
                    title VARCHAR(160) NOT NULL,
                    message VARCHAR(1200) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    read_status BOOLEAN NOT NULL DEFAULT FALSE,
                    archived_status BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (id),
                    KEY idx_animateur_notifications_animateur_id (animateur_id),
                    CONSTRAINT fk_animateur_notifications_animateur
                        FOREIGN KEY (animateur_id) REFERENCES Animateurs(ID)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS admin_notification (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    created_at DATETIME(6) NOT NULL,
                    message VARCHAR(1200) NOT NULL,
                    source VARCHAR(80) NOT NULL,
                    type VARCHAR(80) NOT NULL,
                    PRIMARY KEY (id)
                )
                """);

>>>>>>> origin/main
        if (!columnExists("parent_notifications", "archived_status")) {
            jdbcTemplate.execute("ALTER TABLE parent_notifications ADD COLUMN archived_status BOOLEAN NOT NULL DEFAULT FALSE");
        }

<<<<<<< HEAD
        ensureActivityDateDefaults(isPostgres, isMysql);
        ensureActivityDescriptionColumn(isPostgres, isMysql);

        // --- Création des tables supplémentaires ---
        // Sur PostgreSQL : Hibernate (ddl-auto=update) crée déjà toutes les tables JPA.
        // Ces CREATE TABLE ne sont utiles que pour MySQL/MariaDB ou si Hibernate ne gère pas la table.

        if (isPostgres) {
            // Hibernate gère la création via ddl-auto=update — rien à faire ici.
            return;
        }

        // Blocs MySQL/MariaDB uniquement
        if (isMysql) {
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
                        archived_status BOOLEAN NOT NULL DEFAULT FALSE,
                        PRIMARY KEY (id),
                        KEY idx_parent_notifications_parent_id (parent_id),
                        CONSTRAINT fk_parent_notifications_parent
                            FOREIGN KEY (parent_id) REFERENCES parent(id)
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS animateur_notifications (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        animateur_id BIGINT NOT NULL,
                        category VARCHAR(80) NOT NULL,
                        title VARCHAR(160) NOT NULL,
                        message VARCHAR(1200) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        read_status BOOLEAN NOT NULL DEFAULT FALSE,
                        archived_status BOOLEAN NOT NULL DEFAULT FALSE,
                        PRIMARY KEY (id),
                        KEY idx_animateur_notifications_animateur_id (animateur_id),
                        CONSTRAINT fk_animateur_notifications_animateur
                            FOREIGN KEY (animateur_id) REFERENCES Animateurs(ID)
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS admin_notification (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        created_at DATETIME(6) NOT NULL,
                        message VARCHAR(1200) NOT NULL,
                        source VARCHAR(80) NOT NULL,
                        type VARCHAR(80) NOT NULL,
                        PRIMARY KEY (id)
                    )
                    """);

            ensureInscriptionStatuses();
        }
=======
        ensureInscriptionStatuses();
>>>>>>> origin/main
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

<<<<<<< HEAD
    private void ensureActivityDateDefaults(boolean isPostgres, boolean isMysql) {
        if (isPostgres && tableExists("activity")) {
            ensurePostgresDateDefault("activity", "datecreation");
            ensurePostgresDateDefault("activity", "date_creation");
            return;
        }

        if (isMysql && tableExists("Activity") && columnExists("Activity", "dateCreation")) {
            jdbcTemplate.execute("UPDATE Activity SET dateCreation = CURRENT_DATE WHERE dateCreation IS NULL");
        }
    }

    private void ensureActivityDescriptionColumn(boolean isPostgres, boolean isMysql) {
        if (isPostgres && tableExists("activity") && columnExists("activity", "description")) {
            jdbcTemplate.execute("ALTER TABLE activity ALTER COLUMN description TYPE TEXT");
            return;
        }

        if (isMysql && tableExists("Activity") && columnExists("Activity", "Description")) {
            jdbcTemplate.execute("ALTER TABLE Activity MODIFY COLUMN Description TEXT");
        }
    }

    private void ensurePostgresDateDefault(String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }

        jdbcTemplate.execute("UPDATE " + tableName + " SET " + columnName + " = CURRENT_DATE WHERE " + columnName + " IS NULL");
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET DEFAULT CURRENT_DATE");
    }

    private String detectDbProduct() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        // Essai avec le nom tel quel (minuscules pour PostgreSQL)
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     null,
=======
    // ADDED
    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     connection.getCatalog(),
>>>>>>> origin/main
                     null,
                     tableName,
                     columnName
             )) {
            if (columns.next()) {
                return true;
            }
        } catch (Exception ignored) {
        }

<<<<<<< HEAD
        // Essai en majuscules (compatibilité MySQL / anciens schémas)
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     null,
=======
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     connection.getCatalog(),
>>>>>>> origin/main
                     null,
                     tableName.toUpperCase(),
                     columnName.toUpperCase()
             )) {
            return columns.next();
        } catch (Exception ignored) {
            return false;
        }
    }
<<<<<<< HEAD

    private boolean tableExists(String tableName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet tables = connection.getMetaData().getTables(
                     null,
                     null,
                     tableName,
                     null
             )) {
            if (tables.next()) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet tables = connection.getMetaData().getTables(
                     null,
                     null,
                     tableName.toUpperCase(),
                     null
             )) {
            return tables.next();
        } catch (Exception ignored) {
            return false;
        }
    }
=======
>>>>>>> origin/main
}
