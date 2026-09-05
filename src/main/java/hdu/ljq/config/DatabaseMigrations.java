package hdu.ljq.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Small additive migrations for databases created by earlier project versions. */
@Component
@Order(0)
public class DatabaseMigrations implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public DatabaseMigrations(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    addColumnIfMissing(
        "product", "price_cents", "ALTER TABLE product ADD COLUMN price_cents BIGINT NOT NULL DEFAULT 9900 AFTER category_id");
    addColumnIfMissing(
        "product", "currency", "ALTER TABLE product ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'CNY' AFTER price_cents");
  }

  private void addColumnIfMissing(String table, String column, String statement) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?",
            Integer.class,
            table,
            column);
    if (count != null && count == 0) jdbc.execute(statement);
  }
}
