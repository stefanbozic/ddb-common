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

class CultureGraphService {

    public final static String GND_URI_PREFIX = "http://d-nb.info/gnd/"

    def configurationService

    def transactional=false

    def getCultureGraph(String gndId) {
        ApiResponse apiResponse = ApiConsumer.getJson(configurationService.getCulturegraphUrl(), "/entityfacts/" + gndId)
        if(!apiResponse.isOk()){
            log.error "getCultureGraph(): Could not access culturegraph api under: "+configurationService.getCulturegraphUrl() + "/entityfacts/" + gndId
            return null
        }

        return apiResponse.getResponse()
    }

    boolean isValidGndUri(String uriToTest){
        if(uriToTest != null && uriToTest.startsWith(GND_URI_PREFIX)){
            return true
        }else{
            return false
        }
    }

    String getGndIdFromGndUri(String uri){
        String id = ""
        if(uri != null && uri.startsWith(GND_URI_PREFIX)){
            id = uri.substring(GND_URI_PREFIX.length())
        }
        return id
    }
}
