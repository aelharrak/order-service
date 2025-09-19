package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.PGConnection;
import org.postgresql.replication.PGReplicationStream;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component
public class EventConsumer {
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;
    private PGReplicationStream stream;
    private Connection conn;

    public EventConsumer(ObjectMapper objectMapper, DataSource dataSource) {
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void start() throws Exception {
        try {
            conn = DataSourceUtils.getConnection(dataSource);

            Properties props = new Properties();
            props.setProperty("assumeMinServerVersion", "9.4");
            props.setProperty("replication", "database");
            props.setProperty("preferQueryMode", "simple");

            PGConnection pgConn = conn.unwrap(PGConnection.class);
            stream = pgConn.getReplicationAPI()
                    .replicationStream()
                    .logical()
                    .withSlotName("events_slot")
                    .withStartPosition(org.postgresql.replication.LogSequenceNumber.valueOf(0L))
                    .withSlotOption("include-timestamp", true)
                    .withStatusInterval(10, TimeUnit.SECONDS)
                    .start();

            new Thread(this::consumeStream).start();
        } catch (Exception e) {
            System.err.println("Échec du démarrage de la réplication : " + e.getMessage());
            throw e;
        }
    }

    private void consumeStream() {
        try {
            while (true) {
                ByteBuffer msg = stream.readPending();
                if (msg == null) {
                    Thread.sleep(100);
                    continue;
                }

                String json = new String(msg.array(), msg.position(), msg.remaining());
                System.out.println("Flux d’événements brut : " + json);

                stream.setAppliedLSN(stream.getLastReceiveLSN());
                stream.setFlushedLSN(stream.getLastReceiveLSN());
            }
        } catch (Exception e) {
            System.err.println("Problème dans le flux : " + e.getMessage());
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (stream != null) {
            stream.close();
        }
        if (conn != null) {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}