/*
 * Copyright (C) 2013 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.common


/**
 * Wrapper for all responses of the backend servers.
 * The response itself and additional meta informations about the request and response 
 * are added and available for further computation. The following data is available:
 * 
 * calledUrl: The called URL (e.g. http://backend-p1.deutsche-digitale-bibliothek.de:9998/item/AYKQ6FKHP6A7KFKCK2K3DP6HCVNZQEQC/components/view?client=AP)
 * method: The request method used (GET, POST)
 * content: The requested content type (TEXT, JSON, XML, BINARY)
 * response: The actual response from the server. Dependent of the requested content type, this can contain different object types. (String, InputStream, etc)
 * duration: The duration of the whole backend request
 * exception: If an exception has occured, it will be stored here. This happens also on 404 (ItemNotFoundException) and 500 (BackendErrorException)
 * status: The response status of type ApiResponse.HttpStatus (HTTP_200, HTTP_404, HTTP_500)
 * headers: The response headers from the server
 * 
 * @author hla
 */
class ApiResponse {

    public final static String REQUEST_ATTRIBUTE_APIRESPONSE = "REQUEST_ATTRIBUTE_APIRESPONSE"

    public static enum HttpStatus {
        HTTP_200, HTTP_400, HTTP_401, HTTP_404, HTTP_409, HTTP_500
    }

    def calledUrl
    def method
    def content
    def response
    def duration
    def exception
    def status
    def headers
    def postBody
    def apiKey

    ApiResponse(calledUrl, method, content, response, duration, exception, status, headers, postBody, apiKey){
        this.calledUrl = calledUrl
        this.method = method
        this.content = content
        this.response = response
        this.duration = duration
        this.exception = exception
        this.status = status
        this.headers = headers
        this.postBody = postBody
        this.apiKey = apiKey
    }

    def isOk() {
        return this.status == HttpStatus.HTTP_200
    }

    String toString() {
        def out = "ApiResponse: " + status + " / " + duration + "ms / " + method + " / " + content + " / URL='" + calledUrl + "'"
        if(exception){
            out += " / Exception='" + exception.getMessage() + "'"
        }
        if(postBody){
            out += " / postBody=" + postBody + ""
        }
        if(apiKey){
            out += " / apiKey=" + apiKey.toString().substring(0,5) + "..."
        }
        return out
    }

    def throwException(request){
        request.setAttribute(REQUEST_ATTRIBUTE_APIRESPONSE, this)
        throw this.exception
    }
}
