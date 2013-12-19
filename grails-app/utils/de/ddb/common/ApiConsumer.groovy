/*
 * Copyright (C) 2013 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.common

import grails.util.Holders
import groovy.json.*
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import java.util.regex.Pattern

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

import org.apache.catalina.connector.ClientAbortException
import org.apache.commons.io.IOUtils
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.util.WebUtils

import de.ddb.common.beans.User
import de.ddb.common.exception.AuthorizationException
import de.ddb.common.exception.BackendErrorException
import de.ddb.common.exception.BadRequestException
import de.ddb.common.exception.ConflictException
import de.ddb.common.exception.ItemNotFoundException


/**
 * The main wrapper class for all backend calls.
 *
 * @author hla
 */
class ApiConsumer {

    /**
     * This String will be appended as a parameter to all backend calls. For example: http://aaa.bbb.ccc/ddd?client=DDBNEXT
     */
    public static final String DDBNEXT_CLIENT_NAME = "DDB-NEXT"
    private static int BINARY_BACKEND_TIMEOUT = 5000 // 5s timeout for binary backend requests // TODO move to apd-properties

    private static final log = LogFactory.getLog(this)
    private static Pattern nonProxyHostsPattern

    /**
     * Requests a TEXT ressource from the backend by calling GET
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param optionalHeaders Optional request headers to add to the request
     * @param fixWrongContentTypeHeader Workaround for a bug in the backend. On some responses that contain only application/text, 
     *      the backend sets a response-type of application/json, causing the parser to crash. So if fixWrongContentTypeHeader is set 
     *      to true, the json-parser is explicitly overwritten with the text-parser.  
     * @return An ApiResponse object containing the server response
     */
    static def getText(String baseUrl, String path, boolean httpAuth = false, optionalQueryParams = [:], optionalHeaders = [:], fixWrongContentTypeHeader = false, alreadyEncodedQuery = false) {
        if(fixWrongContentTypeHeader){
            return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.JSON, null, httpAuth, optionalHeaders, true, null, alreadyEncodedQuery)
        }else{
            return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.TEXT, null, httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
        }
    }

    /**
     * Requests a JSON ressource from the backend by calling GET
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def getJson(String baseUrl, String path, boolean httpAuth = false, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.JSON, null, httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Sends a request to the backend by calling POST
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param postParameter body of the POST-Request
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def postJson(String baseUrl, String path, boolean httpAuth = false, postParameter, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.POST, ContentType.JSON, postParameter.toString(), httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Sends a request to the backend by calling PUT
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param putParameter body of the PUT-Request
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def putJson(String baseUrl, String path, boolean httpAuth = false, putParameter, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.PUT, ContentType.JSON, putParameter.toString(), httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Sends a request to the backend by calling PUT
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param putParameter body of the PUT-Request
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def deleteJson(String baseUrl, String path, boolean httpAuth = false, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.DELETE, ContentType.JSON, null, httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Requests a XML ressource from the backend by calling GET
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def getXml(String baseUrl, String path, boolean httpAuth = false, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.XML, null, httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Requests a BINARY ressource as a block WITHOUT STREAMING from the backend by calling GET
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def getBinaryBlock(String baseUrl, String path, boolean httpAuth = false, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.BINARY, null, httpAuth, optionalHeaders, false, null, alreadyEncodedQuery)
    }

    /**
     * Requests a BINARY ressource as a block WITH STREAMING from the backend by calling GET
     * @param baseUrl The base REST-server url
     * @param path The path to the requested resource
     * @param streamingOutputStream The gsp OutputStream needed for streaming binary resources
     * @param optionalHeaders Optional request headers to add to the request
     * @return An ApiResponse object containing the server response
     */
    static def getBinaryStreaming(String baseUrl, String path, boolean httpAuth = false, OutputStream streamingOutputStream, optionalQueryParams = [:], optionalHeaders = [:], alreadyEncodedQuery = false) {
        return requestServer(baseUrl, path, [ client: DDBNEXT_CLIENT_NAME ].plus(optionalQueryParams), Method.GET, ContentType.BINARY, null, httpAuth, optionalHeaders, false, streamingOutputStream, alreadyEncodedQuery)
    }

    /**
     * Central method to call the backend, all other methods just delegate.
     * @param baseUrl The base REST-server url (e.g. "http://backend.escidoc.org")
     * @param path The path to the requested resource (e.g. "/item/ASDGAHASDFASDFDFDGDSVFFDGSDG/view")
     * @param query The query parameters to append to the request (e.g. "[client: 'APD']")
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param optionalHeaders Optional request headers to add to the request
     * @param fixWrongContentTypeHeader Workaround for a bug in the backend. On some responses that contain only application/text, 
     *      the backend sets a response-type of application/json, causing the parser to crash. So if fixWrongContentTypeHeader is set 
     *      to true, the json-parser is explicitly overwritten with the text-parser.  
     * @param streamingOutputStream The gsp OutputStream needed for streaming binary resources
     * @return An ApiResponse object containing the server response
     */
    private static def requestServer(baseUrl, path, query, method, content, requestBody, boolean httpAuth = false, optionalHeaders, fixWrongContentTypeHeader, OutputStream streamingOutputStream, alreadyEncodedQuery = false) {
        def timestampStart = System.currentTimeMillis()
        path = checkContext(baseUrl, path)

        try {
            def http = new HTTPBuilder(baseUrl)

            ProxyUtil proxyUtil = new ProxyUtil()
            proxyUtil.setProxy(http, baseUrl)

            if (httpAuth) {
                setAuthHeader(http)
            }

            //            ParserRegistry registry = http.getParser()
            //            registry.putAt(ContentType.XML) { resp ->
            //                org.apache.http.HttpResponse res = (org.apache.http.HttpResponse)resp
            //
            //                XmlSlurper slurper = new XmlSlurper()
            //                GPathResult result = slurper.parse(res.getEntity().content).declareNamespace("rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#")
            //                return result
            //
            //            }

            // send API key
            def apiKey = setApiKey(optionalHeaders, baseUrl)

            http.request(method, content) { req ->

                if (requestBody != null) {
                    body = requestBody
                }

                if(content == ContentType.BINARY){
                    setTimeout(req)
                }

                uri.path = path
                if(!alreadyEncodedQuery){
                    uri.query = query
                }else{
                    uri.setRawQuery(createQueryStringFromMap(query))
                }

                optionalHeaders.each { key, value ->
                    headers.put(key, value)
                }

                if(content == ContentType.XML){
                    headers.Accept = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
                }
                if(fixWrongContentTypeHeader){
                    http.parser.'application/json' = http.parser.'text/html'
                }

                response.success = { resp, output ->
                    switch(content) {
                        case ContentType.TEXT:
                            return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, output.getText(), requestBody, apiKey)
                        case ContentType.JSON:
                            return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, output, requestBody, apiKey)
                        case ContentType.XML:
                            return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, output, requestBody, apiKey)
                        case ContentType.BINARY:
                            if(streamingOutputStream != null){
                                // We want to stream
                                try{
                                    IOUtils.copy(output, streamingOutputStream)
                                }catch(ClientAbortException c){
                                    log.warn "requestServer(): Client aborted binary request"
                                    return build500Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Client aborted request -> " + uri.toString() + " / " + resp.statusLine + "/"+resp.statusLine.statusCode +"/"+resp.statusLine.reasonPhrase, requestBody, apiKey)
                                }catch(SocketException s){
                                    log.warn "requestServer(): Socket already closed in binary request"
                                    return build500Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Sockets closed -> " + uri.toString() + " / " + resp.statusLine + "/"+resp.statusLine.statusCode +"/"+resp.statusLine.reasonPhrase, requestBody, apiKey)
                                }catch(Throwable t){
                                    log.error "requestServer(): Could not copy streams in binary request: ", t
                                    return build500Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Could not copy streams -> " + uri.toString() + " / " + resp.statusLine + "/"+resp.statusLine.statusCode +"/"+resp.statusLine.reasonPhrase, requestBody, apiKey)
                                }finally{
                                    IOUtils.closeQuietly(output)
                                }

                                return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, ["Content-Type": resp.headers.'Content-Type', "Content-Length": resp.headers.'Content-Length'], requestBody, apiKey)
                            }else{
                                // We don't want to stream
                                return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, [bytes: output.getBytes(), "Content-Type": resp.headers.'Content-Type', "Content-Length": resp.headers.'Content-Length'], requestBody, apiKey)
                            }
                        default:
                            return build200Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, output, requestBody, apiKey)
                    }
                }
                response.'400' = { resp ->
                    return build400Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Server answered 400 -> " + uri.toString() + " / " + resp.headers.'Error-Message', requestBody, apiKey)
                }
                response.'401' = { resp ->
                    return build401Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Server answered 401 -> " + uri.toString() + " / " + resp.headers.'Error-Message', requestBody, apiKey)
                }
                response.'404' = { resp, output ->
                    return build404Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Server answered 404 -> " + uri.toString() + " / " + resp.headers.'Error-Message', output, requestBody, apiKey)
                }
                response.'409' = { resp ->
                    return build409Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Server answered 409 -> " + uri.toString() + " / " + resp.headers.'Error-Message', requestBody, apiKey)
                }
                response.failure = { resp ->
                    return build500Response(timestampStart, uri.toString(), method.toString(), content.toString(), resp.headers, "Server answered 500 -> " + uri.toString() + " / " + resp.statusLine + "/"+resp.statusLine.statusCode +"/"+resp.statusLine.reasonPhrase+"/"+resp.headers.'Error-Message', requestBody, apiKey)
                }
            }
        } catch (groovyx.net.http.HttpResponseException ex) {
            log.error "requestText(): A HttpResponseException occured", ex
            return build500ResponseWithException(timestampStart, baseUrl + path + query, method.toString(), content.toString(), ex, requestBody, null)
        } catch (java.net.ConnectException ex) {
            log.error "requestText(): A ConnectException occured", ex
            return build500ResponseWithException(timestampStart, baseUrl + path + query, method.toString(), content.toString(), ex, requestBody, null)
        } catch (java.lang.Exception ex) {
            log.error "requestText(): An Exception occured", ex
            return build500ResponseWithException(timestampStart, baseUrl + path + query, method.toString(), content.toString(), ex, requestBody, null)
        }
    }

    /**
     * Utility method to build the ApiResponse object for successfull requests
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param responseObject The response from the server
     * @return An ApiResponse object containing the server response
     */
    private static def build200Response(timestampStart, calledUrl, method, content, responseHeader, responseObject, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        def response = new ApiResponse(calledUrl, method.toString(), content, responseObject, duration, null, ApiResponse.HttpStatus.HTTP_200, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 400 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param exceptionDescription The text for the ItemNotFoundException that will be attached but not thrown
     * @return An ApiResponse object containing the response information
     */
    private static def build400Response(timestampStart, calledUrl, method, content, responseHeader, exceptionDescription, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        BadRequestException exception = new BadRequestException(exceptionDescription)
        def response = new ApiResponse(calledUrl, method.toString(), content, "", duration, exception, ApiResponse.HttpStatus.HTTP_400, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 401 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param exceptionDescription The text for the ItemNotFoundException that will be attached but not thrown
     * @return An ApiResponse object containing the response information
     */
    private static def build401Response(timestampStart, calledUrl, method, content, responseHeader, exceptionDescription, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        AuthorizationException exception = new AuthorizationException(exceptionDescription)
        def response = new ApiResponse(calledUrl, method.toString(), content, "", duration, exception, ApiResponse.HttpStatus.HTTP_401, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 404 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param exceptionDescription The text for the ItemNotFoundException that will be attached but not thrown
     * @return An ApiResponse object containing the response information
     */
    private static def build404Response(timestampStart, calledUrl, method, content, responseHeader, exceptionDescription, responseObject, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        ItemNotFoundException exception = new ItemNotFoundException(exceptionDescription)
        def response = new ApiResponse(calledUrl, method.toString(), content, responseObject, duration, exception, ApiResponse.HttpStatus.HTTP_404, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 409 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param exceptionDescription The text for the ConflictException that will be attached but not thrown
     * @return An ApiResponse object containing the response information
     */
    private static def build409Response(timestampStart, calledUrl, method, content, responseHeader, exceptionDescription, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        ConflictException exception = new ConflictException(exceptionDescription)
        def response = new ApiResponse(calledUrl, method.toString(), content, "", duration, exception, ApiResponse.HttpStatus.HTTP_409, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 500 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param responseHeader The headers of the response
     * @param exceptionDescription The text for the BackendErrorException that will be attached but not thrown
     * @return An ApiResponse object containing the response information
     */
    private static def build500Response(timestampStart, calledUrl, method, content, responseHeader, exceptionDescription, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        BackendErrorException exception = new BackendErrorException(exceptionDescription)
        def response = new ApiResponse(calledUrl, method.toString(), content, "", duration, exception, ApiResponse.HttpStatus.HTTP_500, responseHeader, postBody, apiKey)
        log.info response.toString()
        return response
    }

    /**
     * Utility method to build the ApiResponse object for 500 responses
     * @param timestampStart The timestamp when the request was send
     * @param calledUrl The complete URL that was called
     * @param method The request method (Method.GET, Method.POST)
     * @param content The expected response content (ContentType.TEXT, ContentType.JSON, ContentType.XML, ContentType.BINARY)
     * @param exceptionDescription The actual exception that occured
     * @return An ApiResponse object containing the response information
     */
    private static def build500ResponseWithException(timestampStart, calledUrl, method, content, exception, postBody, apiKey){
        def duration = System.currentTimeMillis()-timestampStart
        def response = new ApiResponse(calledUrl, method.toString(), content, "", duration, exception, ApiResponse.HttpStatus.HTTP_500, [:], postBody, apiKey)
        log.info response.toString()
        return response
    }


    /**
     * Sets explicit timeout parameters to requests.
     * @param req the requests object
     * @return void
     */
    private static def setTimeout(def req){
        try {
            req.getParams().setParameter("http.connection.timeout", new Integer(BINARY_BACKEND_TIMEOUT))
            req.getParams().setParameter("http.socket.timeout", new Integer(BINARY_BACKEND_TIMEOUT))
        } catch(Exception e) {
            log.error "setTimeout(): Could not set the timeout to the binary request"
        }
    }

    static def checkContext(String baseUrl, String path) {
        def grailsApplication = Holders.getGrailsApplication()
        if (grailsApplication.config.ddb.apis.url == baseUrl) {
            if (path == null) {
                path = ""
            }
            path = ServletContextHolder.servletContext.contextPath + path
        }
        return path
    }

    static String setApiKey(optionalHeaders, baseUrl) {
        def grailsApplication = Holders.getGrailsApplication()
        if (baseUrl.startsWith(grailsApplication.config.ddb.backend.url) && grailsApplication.config.ddb.backend.apikey) {
            optionalHeaders.put("Authorization", 'OAuth oauth_consumer_key="' + grailsApplication.config.ddb.backend.apikey + '"')
            return grailsApplication.config.ddb.backend.apikey
        }
        return null
    }

    static def setAuthHeader(http){
        try {
            HttpServletRequest request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
            HttpSession session = request.getSession(false)
            if(session) {
                User user = session.getAttribute(User.SESSION_USER)
                if (user != null) {
                    http.auth.basic user.id, user.password
                }
            }
        } catch(Exception e) {
            log.error "setAuthHeader(): Could not get haeder-data from session"
        }
    }

    static String createQueryStringFromMap(query){
        String out = ""
        if(query){
            query.each {
                out += "&"+it.key+"="+it.value
            }
        }
        if(out.startsWith("&")){
            out = out.substring(1)
        }
        return out
    }
}
