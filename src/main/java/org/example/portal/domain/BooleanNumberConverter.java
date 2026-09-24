package org.example.portal.domain;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Oracle 11g has no SQL BOOLEAN type. */
@Converter(autoApply=true)
public class BooleanNumberConverter implements AttributeConverter<Boolean,Integer> {
    @Override public Integer convertToDatabaseColumn(Boolean value) { return value==null ? null : value ? 1 : 0; }
    @Override public Boolean convertToEntityAttribute(Integer value) { return value==null ? null : value!=0; }
}
