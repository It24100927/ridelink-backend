package com.ridelink.ride_management_service.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:ride_db}")
    private String database;

    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    /**
     * Registers explicit converters for LocalDateTime.
     *
     * Two reading converters are needed:
     *  1. Date -> LocalDateTime: for documents saved WITH the write converter (BSON ISODate)
     *  2. Document -> LocalDateTime: for OLD documents saved WITHOUT the write converter,
     *     where Spring Data stored LocalDateTime as a BSON subdocument
     *     e.g. {"year":2026,"monthValue":9,"dayOfMonth":24,"hour":10,...}
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new DateToLocalDateTimeConverter(),
                new DocumentToLocalDateTimeConverter(),
                new LocalDateTimeToDateConverter()
        ));
    }

    /** Reads BSON ISODate (java.util.Date) -> LocalDateTime */
    @ReadingConverter
    static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    /**
     * Reads BSON subdocument -> LocalDateTime.
     * Handles documents saved by Spring Data's default serialization before
     * the write converter was registered.
     */
    @ReadingConverter
    static class DocumentToLocalDateTimeConverter implements Converter<Document, LocalDateTime> {
        @Override
        public LocalDateTime convert(Document source) {
            try {
                return LocalDateTime.of(
                        source.getInteger("year", 1970),
                        source.getInteger("monthValue", 1),
                        source.getInteger("dayOfMonth", 1),
                        source.getInteger("hour", 0),
                        source.getInteger("minute", 0),
                        source.getInteger("second", 0),
                        source.getInteger("nano", 0)
                );
            } catch (Exception e) {
                return null;
            }
        }
    }

    /** Writes LocalDateTime -> BSON ISODate (java.util.Date) */
    @WritingConverter
    static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
        }
    }
}