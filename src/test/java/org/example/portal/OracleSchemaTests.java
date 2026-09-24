package org.example.portal;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

// Generates Oracle DDL from the mappings without connecting to an Oracle server.
@SpringBootTest(properties={
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.database-platform=org.hibernate.community.dialect.OracleLegacyDialect",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "spring.jpa.properties.jakarta.persistence.database-product-name=Oracle",
    "spring.jpa.properties.jakarta.persistence.database-major-version=11",
    "spring.jpa.properties.jakarta.persistence.database-minor-version=2",
    "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/oracle-schema-generated.sql",
    "spring.jpa.properties.hibernate.hbm2ddl.schema-generation.script.append=false"
})
@ActiveProfiles("test")
class OracleSchemaTests {
    @Autowired EntityManagerFactory factory;
    @Test void targetsOracle11() throws java.io.IOException {
        var dialect=factory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        assertThat(dialect.getClass().getSimpleName()).isEqualTo("OracleLegacyDialect");
        assertThat(dialect.getVersion().getMajor()).isEqualTo(11);
        assertThat(dialect.getVersion().getMinor()).isEqualTo(2);
        var ddl=java.nio.file.Files.readString(java.nio.file.Path.of("target/oracle-schema-generated.sql"));
        assertThat(ddl).contains("start_time varchar2(","content clob", "create sequence portal_seq");
        assertThat(ddl).doesNotContain(" time("," boolean", "generated as identity");
    }
}
