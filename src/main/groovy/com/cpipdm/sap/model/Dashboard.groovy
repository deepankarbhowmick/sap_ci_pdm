package com.cpipdm.sap.model

import io.micronaut.core.annotation.Introspected

@Introspected
class Dashboard {
    String username, password, tmn, SystemId
    ArrayList<LinkedHashMap> table
    LinkedHashMap filter
}
