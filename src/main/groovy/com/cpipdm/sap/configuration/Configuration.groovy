package com.cpipdm.sap.configuration

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Property
import io.micronaut.core.convert.format.MapFormat

import javax.inject.Singleton

@Singleton
@ConfigurationProperties('micronaut.sap')
class Configuration {
    ArrayList<String> entity
    ArrayList<String> readOnly

    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    Map<String, String> tenant
    ArrayList<HashMap> keyField

    FieldList fieldList = new FieldList()

    @ConfigurationProperties('field-list')
    static class FieldList {
        ArrayList<String> stringparameters,
                          binaryparameters,
                          alternativepartners,
                          authorizedusers,
                          filetype
    }
}
