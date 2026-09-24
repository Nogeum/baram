package org.example.portal.domain;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Oracle 11g has no standalone SQL TIME; schedule times use HH:mm:ss. */
@Converter(autoApply=true)
public class LocalTimeTextConverter implements AttributeConverter<LocalTime,String> {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("HH:mm:ss");
    @Override public String convertToDatabaseColumn(LocalTime value) { return value==null ? null : value.format(FORMAT); }
    @Override public LocalTime convertToEntityAttribute(String value) { return value==null ? null : LocalTime.parse(value,FORMAT); }
}
