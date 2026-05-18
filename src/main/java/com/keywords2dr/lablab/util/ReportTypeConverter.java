package com.keywords2dr.lablab.util;

import com.keywords2dr.lablab.entity.enums.ReportType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ReportTypeConverter implements Converter<String, ReportType> {

    @Override
    public ReportType convert(String source) {
        if (source == null || source.isBlank()) return null;
        return ReportType.valueOf(source.trim().toUpperCase());
    }
}