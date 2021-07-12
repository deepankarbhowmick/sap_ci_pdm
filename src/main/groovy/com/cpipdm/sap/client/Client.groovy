package com.cpipdm.sap.client

import io.micronaut.http.BasicAuth
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.reactivex.Single

@io.micronaut.http.client.annotation.Client
interface Client {

    @Get(value = '{+hostname}/api/v1/$metadata',
            consumes = MediaType.APPLICATION_XML)
    Single<HttpResponse> getMetaData(@PathVariable('hostname') String hostName,
                                     BasicAuth basicAuth) //Get metadata.

    @Get(value = '{+hostname}/api/v1/{entityName}',
            consumes = MediaType.APPLICATION_JSON)
    Single<HttpResponse> getToken(@Header('x-csrf-token') String token,
                                  @PathVariable('hostname') String hostName,
                                  @PathVariable('entityName') String entityName) //Get x-csrf-token.

    @Put(value = '{+hostname}/api/v1/{entityName}({+primaryKey})',
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON)
    Single<HttpResponse> update(@Header('x-csrf-token') String token,
                                @PathVariable('hostname') String hostName,
                                @PathVariable('entityName') String entityName,
                                @PathVariable('primaryKey') String primaryKey,
                                @CookieValue('__Host-csrf-client-id') String cookieValue,
                                @Body Map body) //Update.

    @Delete(value = '{+hostname}/api/v1/{entityName}({+primaryKey})')
    Single<HttpResponse> delete(@Header('x-csrf-token') String token,
                                @PathVariable('hostname') String hostName,
                                @PathVariable('entityName') String entityName,
                                @PathVariable('primaryKey') String primaryKey,
                                @CookieValue('__Host-csrf-client-id') String cookieValue) //Delete.

    @Post(value = '{+hostname}/api/v1/{entityName}',
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON)
    Single<HttpResponse> create(@Header('x-csrf-token') String token,
                                @PathVariable('hostname') String hostName,
                                @PathVariable('entityName') String entityName,
                                @CookieValue('__Host-csrf-client-id') String cookieValue,
                                @Body Map body) //Create.
}