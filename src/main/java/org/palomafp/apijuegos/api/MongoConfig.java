package org.palomafp.apijuegos.api;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

/**
 * Configuración de la conexión a MongoDB Atlas.
 *
 * <p>Sustituye a la autoconfiguración de Spring Data para poder inyectar las
 * credenciales por variable de entorno en lugar de leerlas del fichero
 * {@code .env} que se genera en el servidor.</p>
 *
 * @author Andrés López
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    // Spring inyectará automáticamente los valores de tu .env o GitHub Secrets aquí
    @Value("${MONGO_USER}")
    private String dbUser;

    @Value("${MONGO_PASS}")
    private String dbPass;

    @Value("${MONGO_DB}")
    private String dbName;

    @Override
    @NullMarked
    protected String getDatabaseName() {
        return dbName; // Usamos la variable para el nombre de la BD
    }

    @Override
    @NullMarked
    public MongoClient mongoClient() {
        logger.info("Forzando conexión manual a Atlas con las credenciales inyectadas");

        // Construimos la URL usando las variables inyectadas por Spring.
        // Text block: la URI es larga y queda mucho más legible partida en líneas
        // que encadenada con signos +.
        String uri = """
                mongodb+srv://%s:%s@bd-apijuegos.v0ynuuf.mongodb.net/%s\
                ?retryWrites=true&w=majority&appName=bd-apiJuegos"""
                .formatted(dbUser, dbPass, dbName);

        var connectionString = new ConnectionString(uri);

        var mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }
}
