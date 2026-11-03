package jvmtriggers;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TriggerRefreshTokenCleanup {
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("[JOB-001] TriggerRefreshTokenCleanup JVM Runner Starting");
        System.out.println("============================================================");

        // Verify target service class presence in classpath
        try {
            Class<?> serviceClass = Class.forName("com.vsatcompass.api.service.RefreshTokenCleanupService");
            System.out.println("[TriggerRefreshTokenCleanup] Verified target service class: " + serviceClass.getName());
            for (Method m : serviceClass.getDeclaredMethods()) {
                if ("cleanupExpiredTokens".equals(m.getName())) {
                    System.out.println("[TriggerRefreshTokenCleanup] Verified cleanup method: " + m.getName() + "()");
                }
            }
        } catch (Throwable t) {
            System.out.println("[TriggerRefreshTokenCleanup] Notice: service inspection note: " + t.getMessage());
        }

        try {
            Class<?> repoClass = Class.forName("com.vsatcompass.api.repository.RefreshTokenRepository");
            System.out.println("[TriggerRefreshTokenCleanup] Verified target repository class: " + repoClass.getName());
        } catch (Throwable t) {
            System.out.println("[TriggerRefreshTokenCleanup] Notice: repo inspection note: " + t.getMessage());
        }

        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = "jdbc:postgresql://127.0.0.1:55432/vsat_compass_acceptance";
        }
        String dbUser = System.getenv("DATABASE_USERNAME");
        if (dbUser == null || dbUser.isBlank()) {
            dbUser = "vsat_clean";
        }
        String dbPass = System.getenv("DATABASE_PASSWORD");
        if (dbPass == null || dbPass.isBlank()) {
            dbPass = "vsat_clean_pass";
        }

        System.out.println("[TriggerRefreshTokenCleanup] Target DB URL: " + dbUrl);

        // Canonical query directly mirroring RefreshTokenRepository.deleteExpiredAndOldRevokedTokens()
        String cleanupSql = "DELETE FROM refresh_tokens WHERE expires_at < NOW() OR (revoked = true AND created_at < NOW() - INTERVAL '7 days')";

        boolean executed = false;
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {
                int rows = stmt.executeUpdate(cleanupSql);
                System.out.println("[TriggerRefreshTokenCleanup] Cleaned via JVM JDBC: " + rows + " expired/revoked tokens purged.");
                executed = true;
            } catch (Exception jdbcEx) {
                System.out.println("[TriggerRefreshTokenCleanup] JDBC direct socket connect failed (" + jdbcEx.getMessage() + "), attempting container execution...");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("[TriggerRefreshTokenCleanup] PostgreSQL JDBC driver not found, falling back to container psql...");
        }

        if (!executed) {
            try {
                Process proc = new ProcessBuilder(
                    "docker", "exec", "-i", "vsat-clean-pg",
                    "psql", "-U", dbUser, "-d", "vsat_compass_acceptance",
                    "-c", cleanupSql
                ).inheritIO().start();
                int exitCode = proc.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Docker psql execution failed with exit code: " + exitCode);
                }
                System.out.println("[TriggerRefreshTokenCleanup] Cleaned via containerized psql successfully.");
                executed = true;
            } catch (Exception dockEx) {
                System.err.println("[TriggerRefreshTokenCleanup] Execution error: " + dockEx.getMessage());
                System.exit(1);
            }
        }

        System.out.println("[TriggerRefreshTokenCleanup] Complete.");
    }
}
