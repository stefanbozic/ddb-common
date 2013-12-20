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

import static groovyx.net.http.ContentType.*
import groovy.json.*
import de.ddb.common.constants.FacetEnum
import de.ddb.common.constants.SearchParamEnum

/**
 * Set of services used in the ApisController for views/search
 *
 * @author ema
 *
 */

class ApisService {

    //Autowire the grails application bean
    def grailsApplication
    def configurationService

    def transactional=false

    /**
     * Transform the query parameter Map from the SearchService to a backend compatible query Map  
     * 
     * @param queryParameters The map with the original frontend API query parameter
     * 
     * @return query parameter map for the cortex API
     */
    def getQueryParameters(Map queryParameters){

        def query = [ (SearchParamEnum.QUERY.getName()): queryParameters.query ]

        if(queryParameters[SearchParamEnum.OFFSET.getName()])
            query[SearchParamEnum.OFFSET.getName()]= queryParameters[SearchParamEnum.OFFSET.getName()]

        if(queryParameters[SearchParamEnum.ROWS.getName()])
            query[SearchParamEnum.ROWS.getName()] = queryParameters[SearchParamEnum.ROWS.getName()]

        if(queryParameters[SearchParamEnum.CALLBACK.getName()])
            query[SearchParamEnum.CALLBACK.getName()] = queryParameters[SearchParamEnum.CALLBACK.getName()]

        if(queryParameters[SearchParamEnum.FACET.getName()]){
            if(queryParameters[SearchParamEnum.FACET.getName()].getClass().isArray()){
                query[SearchParamEnum.FACET.getName()] = []
                queryParameters[SearchParamEnum.FACET.getName()].each {
                    query[SearchParamEnum.FACET.getName()].add(it)
                }
            }else query[SearchParamEnum.FACET.getName()]=queryParameters[SearchParamEnum.FACET.getName()]
        }

        if(queryParameters[SearchParamEnum.MINDOCS.getName()])
            query[SearchParamEnum.MINDOCS.getName()] = queryParameters[SearchParamEnum.MINDOCS.getName()]

        if(queryParameters[SearchParamEnum.SORT.getName()])
            query[SearchParamEnum.SORT.getName()] = queryParameters[SearchParamEnum.SORT.getName()]

        //Evaluates the facetValues from the API request
        FacetEnum.values().each() {
            evaluateFacetParameter(query, queryParameters[it.getName()], it.getName())
        }


        if(queryParameters.grid_preview){
            query["grid_preview"]=queryParameters.grid_preview
        }

        if(queryParameters["facet.limit"]){
            query["facet.limit"] = queryParameters["facet.limit"]
        }

        return query
    }

    /**
     * 
     * @param query
     * @param queryParameter
     * @param facetName
     * @return
     */
    private def evaluateFacetParameter(Map query, Object queryParameter, String facetName) {
        if(queryParameter){
            if(queryParameter.getClass().isArray()){
                query[facetName] = []
                queryParameter.each {
                    query[facetName].add(it)
                }
            }else {
                query[facetName]=queryParameter
            }
        }
    }

}
