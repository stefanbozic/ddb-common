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

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET

import org.codehaus.groovy.grails.web.util.WebUtils

/**
 * Get facetted searchfields and values for facet from Backend.
 * 
 * @author mih
 *
 */
public class FacetsService {

    def configurationService

    def transactional = false

    /**
     * Get values for a facet.
     * 
     * @param facetName The name of the facet
     * @param allFacetFilters List of all available facet filter mappings
     * @return List of Facet-Values
     */
    public List getFacet(facetName, allFacetFilters) throws IOException {
        def url = configurationService.getBackendUrl()
        def filtersForFacetName = getFiltersForFacetName(facetName, allFacetFilters)
        def res = []
        int i = 0
        def apiResponse = ApiConsumer.getJson(url ,'/search/facets/' + facetName)
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        def json = apiResponse.getResponse()
        json.facetValues.each{
            if(filtersForFacetName.isEmpty() || !filtersForFacetName.contains(it.value)){
                res[i] = it.value
                i++
            }
        }
        return res
    }

    /**
     * Get List of Searchfields(Facets) for advanced search.
     * 
     * @return List of Arrays.<br>
     *     Array contains name, searchType(TEXT or ENUM), sortType(ALPHA_ID, ALPHA_LABEL).<br>
     *     name: name of facet(Searchfield).<br>
     *     searchType TEXT: display textfield for searchstring.<br>
     *     searchType ENUM: display selectbox with possible values for searchstring.<br>
     *     sortType (for searchType = ENUM): how to sort possible values.
     *     ALPHA_ID: sort by id.<br>
     *     ALPHA_LABEL: sort by value.<br>
     */
    public List getExtendedFacets() throws IOException {
        def url = configurationService.getBackendUrl()
        def res = [];
        def apiResponse = ApiConsumer.getJson(url ,'/search/facets/', false, [type:'EXTENDED'])
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        def json = apiResponse.getResponse()
        json.each{
            def part = [:]
            part["name"] = it.name
            part["searchType"] = it.searchType
            part["sortType"] = it.sortType
            res[it.position - 1] = part
        }
        return res
    }


    /**
     * Takes a list of configured facet filter mapping and returns only the filter values for the matching facet name.
     * E.g.: facetName=facet1, allFacetsFilters=[{facetName:facet1, filter:filter1}, {facetName:facet2, filter:filter2}]
     * The returned list would be [filter1]
     * @param facetName The name of the facet 
     * @param allFacetFilters List of mappings containing all available facet filter mappings
     * @return A list of filters for the matching facet name
     */
    private List getFiltersForFacetName(facetName, allFacetFilters){
        def filtersForFacetName = []
        for(filter in allFacetFilters) {
            if(filter.facetName != null && filter.facetName.equals(facetName)){
                filtersForFacetName.add(filter.filter)
            }
        }
        return filtersForFacetName;
    }
}
