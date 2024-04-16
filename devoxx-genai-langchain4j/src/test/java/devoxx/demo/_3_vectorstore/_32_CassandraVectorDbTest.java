package devoxx.demo._3_vectorstore;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import devoxx.demo.devoxx.Product;
import devoxx.demo.utils.AbstractDevoxxTestSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class _32_CassandraVectorDbTest extends AbstractDevoxxTestSupport {

    Logger log = LoggerFactory.getLogger(_32_CassandraVectorDbTest.class);

    @Test
    @Disabled
    public void cassandraVectorSearchTest() throws IOException {

        // Connection to the Cassandra
        try (CqlSession cqlSession = getCassandraSession()) {
            log.info("Connected to Cassandra");

                // Create a Table with Embeddings
                cqlSession.execute(
                        "CREATE TABLE IF NOT EXISTS pet_supply_vectors (" +
                                "    product_id     TEXT PRIMARY KEY," +
                                "    product_name   TEXT," +
                                "    product_vector vector<float, 14>)");
                log.info("Table created.");

                // Create a Search Index
                cqlSession.execute(
                        "CREATE CUSTOM INDEX IF NOT EXISTS idx_vector " +
                                "ON pet_supply_vectors(product_vector) " +
                                "USING 'StorageAttachedIndex'");
                log.info("Index Created.");

                // Insert rows
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pf1843','HealthyFresh - Chicken raw dog food',[1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0])");
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pf1844','HealthyFresh - Beef raw dog food',[1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0])");
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pt0021','Dog Tennis Ball Toy',[0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0])");
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pt0041','Dog Ring Chew Toy',[0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0])");
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pf7043','PupperSausage Bacon dog Treats',[0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1])");
                cqlSession.execute(
                        "INSERT INTO pet_supply_vectors (product_id, product_name, product_vector) " +
                                "VALUES ('pf7044','PupperSausage Beef dog Treats',[0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0])");

                // Find By ID (primary KEY)
                Row row = cqlSession.execute(SimpleStatement
                        .builder("SELECT * FROM pet_supply_vectors WHERE product_id = ?")
                        .addPositionalValue("pf1843").build()).one();
                Product p = Optional.ofNullable(row)
                        .map(this::mapCassandraRow2Product)
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                log.info("Product Found ! looking for similar products");

                // Semantic Search
               ResultSet resultSet = cqlSession.execute(SimpleStatement
                       .builder("SELECT * FROM pet_supply_vectors " +
                               "ORDER BY product_vector ANN OF ? LIMIT 2;")
                       .addPositionalValue(p.vector())
                       .build());
                List<Product> similarProducts = resultSet.all()
                        .stream().map(this::mapCassandraRow2Product).toList();
                log.info("Similar Products : {}", similarProducts);
            }
        }

    private Product mapCassandraRow2Product(Row row) {
        return new Product(
                row.getString("product_id"),
                row.getString("product_name"),
                row.getObject("product_vector"));
    }


}
