
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

import groovy.json.JsonSlurper

import java.util.regex.Pattern

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.context.i18n.LocaleContextHolder

import de.ddb.common.constants.CortexConstants
import de.ddb.common.constants.FacetEnum
import de.ddb.common.constants.SearchParamEnum

/**
 * Set of services used in the SearchController for views/search
 * 
 * @author ema
 *
 */

class SearchService {

    //Autowire the grails application bean
    def grailsApplication

    def configurationService

    //CharacterEncoding of query-String
    private static final String CHARACTER_ENCODING = "UTF-8"

    //Name of search-cookie
    private searchCookieName = "searchParameters"

    //FIXME get this list from the FacetEnum
    private static facetsList = [
        FacetEnum.TIME.getName(),
        FacetEnum.PLACE.getName(),
        FacetEnum.AFFILIATE.getName(),
        FacetEnum.KEYWORDS.getName(),
        FacetEnum.LANGUAGE.getName(),
        FacetEnum.TYPE.getName(),
        FacetEnum.SECTOR.getName(),
        FacetEnum.PROVIDER.getName()
    ]

    def transactional=false

    def getFacets(Map reqParameters, Map urlQuery, String key, int currentDepth){
        List facetValues = []
        def facets = urlQuery
        facets[SearchParamEnum.FACET.getName()] = []
        if(reqParameters.get(SearchParamEnum.FACETVALUES.getName()).getClass().isArray()){
            reqParameters.get(SearchParamEnum.FACETVALUES.getName()).each{ facetValues.add(it) }
        }else{
            facetValues.add(reqParameters.get(SearchParamEnum.FACETVALUES.getName()).toString())
        }
        facetValues.each {
            def tmpVal = java.net.URLDecoder.decode(it.toString(), "UTF-8")
            List tmpSubVal = tmpVal.split("=")
            if(!facets[SearchParamEnum.FACET.getName()].contains(tmpSubVal[0]))
                facets[SearchParamEnum.FACET.getName()].add(tmpSubVal[0].toString())
            if(!facets[tmpSubVal[0]]){
                facets[tmpSubVal[0]]=[tmpSubVal[1]]
            }else
                facets[tmpSubVal[0]].add(tmpSubVal[1])
        }
        return facets
    }

    /**
     * This method converts the "facetValues[]" parameter of a request to an url encoded query String
     * 
     * @param reqParameters the requestParameter
     * @return the url encoded query String for facetValues parameter
     */
    def facetValuesToUrlQueryString(GrailsParameterMap reqParameters){
        def res = ""
        def facetValues = facetValuesRequestParameterToList(reqParameters)

        if(facetValues != null){
            res = facetValuesToUrlQueryString(facetValues)
        }

        return res
    }

    /**
     * This method converts a list of facetValues to an url encoded query String
     * 
     * @param facetValues the List of facetValues
     * @return the url encoded query String for facetValues parameter
     */
    def facetValuesToUrlQueryString(List facetValues){
        def res = ""

        facetValues.each{
            res += "&facetValues%5B%5D="+it.encodeAsURL()
        }

        return res
    }

    /**
     * This methods get all "facetValues[]" parameter from the request and returns them as a list
     * 
     * @param reqParameters the request parameter map
     * @return a list with all "facetValues[]" parameter
     */
    def facetValuesRequestParameterToList(GrailsParameterMap reqParameters) {
        def urlFacetValues = []
        def requestParamValues = reqParameters.get(SearchParamEnum.FACETVALUES.getName())

        if (requestParamValues != null){
            //The facetValues request parameter could be of type Array or String
            if(requestParamValues.getClass().isArray()){
                urlFacetValues = requestParamValues as List
            }else{
                urlFacetValues.add(requestParamValues)
            }
        }

        return urlFacetValues
    }

    /**
     * Creates the urls for the main facets of the non js version of the facet search
     * 
     * 
     * @param reqParameters the request parameter
     * @param urlQuery the urlQuery object
     * @param requestObject the request object
     * 
     * @return a map containing the facet name as key and the url as value 
     */
    def buildMainFacetsUrl(GrailsParameterMap reqParameters, LinkedHashMap urlQuery, HttpServletRequest requestObject){
        def mainFacetsUrls = [:]

        facetsList.each {
            def searchQuery = (urlQuery[SearchParamEnum.QUERY.getName()]) ? urlQuery[SearchParamEnum.QUERY.getName()] : ""

            //remove the main facet from the URL (the main facet is selected in this request)
            if(urlQuery[SearchParamEnum.FACET.getName()] && urlQuery[SearchParamEnum.FACET.getName()].contains(it)){
                mainFacetsUrls.put(it,requestObject.forwardURI+'?'+SearchParamEnum.QUERY.getName()+'='+searchQuery+"&"+SearchParamEnum.OFFSET.getName()+"=0&"+SearchParamEnum.ROWS.getName()+"="+urlQuery[SearchParamEnum.ROWS.getName()]+facetValuesToUrlQueryString(reqParameters))
            }
            //add the main facet from the URL (the main facet is deselected in this request)
            else{
                mainFacetsUrls.put(it,requestObject.forwardURI+'?'+SearchParamEnum.QUERY.getName()+'='+searchQuery+"&"+SearchParamEnum.OFFSET.getName()+"=0&"+SearchParamEnum.ROWS.getName()+"="+urlQuery[SearchParamEnum.ROWS.getName()]+"&facets%5B%5D="+it+facetValuesToUrlQueryString(reqParameters))
            }
        }

        return mainFacetsUrls
    }

    /**
     * Creates the urls for the sub facets of the non JS version of the facet search
     * 
     * @param reqParameters the request parameter
     * @param facets the list of available facets for this search request
     * @param mainFacetsUrl the urls of the main facets
     * @param urlQuery the urlQuery
     * @param requestObject the request object from the controller
     * 
     * @return a map containing the main facet name as key and a map as value (containing all subfacets storing the name, count and url)  
     */
    def buildSubFacetsUrl(GrailsParameterMap reqParameters, List facets, LinkedHashMap mainFacetsUrl, LinkedHashMap urlQuery, HttpServletRequest requestObject){
        def searchQuery = (urlQuery[SearchParamEnum.QUERY.getName()]) ? urlQuery[SearchParamEnum.QUERY.getName()] : ""

        def res = [:]
        urlQuery[SearchParamEnum.FACET.getName()].each{
            if(it!="grid_preview"){
                facets.each { x->
                    if(x.field == it && x.numberOfFacets>0){
                        res[x.field] = []
                        x.facetValues.each{ y->
                            //only proceed if the facetValue is of type main facet. Role facets will be ignored
                            if (mainFacetsUrl[x.field] != null) {

                                //Create a map which contains the facet name, count and url for the view
                                def tmpFacetValuesMap = ["fctValue": y["value"].encodeAsHTML(),"url":"",cnt: y["count"],selected:""]

                                //Convert the facetValues[] parameter of the request from an array/string to a list. List entries can be changed (add/remove)!
                                def urlFacetValues = facetValuesRequestParameterToList(reqParameters)

                                //The current facetValue in the target request parameter form
                                def facetValueParameter = x.field+"="+y["value"]

                                if(urlFacetValues.contains(facetValueParameter)){
                                    //remove the facetValueParameter from the urlFacetValues (the facet was selected in this request)
                                    urlFacetValues.remove(facetValueParameter)
                                    def url = requestObject.forwardURI+'?'+SearchParamEnum.QUERY.getName()+'='+searchQuery+"&"+SearchParamEnum.OFFSET.getName()+"=0&"+SearchParamEnum.ROWS.getName()+"="+urlQuery[SearchParamEnum.ROWS.getName()]+"&facets%5B%5D="+x.field+facetValuesToUrlQueryString(urlFacetValues)

                                    tmpFacetValuesMap["url"] = url
                                    tmpFacetValuesMap["selected"] = "selected"
                                }
                                else{
                                    //add the facetValueParameter to the urlFacetValues (the facet was deselected in this request)
                                    urlFacetValues.add(facetValueParameter)

                                    def url = requestObject.forwardURI+'?'+SearchParamEnum.QUERY.getName()+'='+searchQuery+"&"+SearchParamEnum.OFFSET.getName()+"=0&"+SearchParamEnum.ROWS.getName()+"="+urlQuery[SearchParamEnum.ROWS.getName()]+"&facets%5B%5D="+x.field+facetValuesToUrlQueryString(urlFacetValues)
                                    tmpFacetValuesMap["url"] = url
                                }

                                res[x.field].add(tmpFacetValuesMap)
                            }
                        }
                    }
                }
            }
        }
        return res
    }


    /**
     * Creates the urls for the rolefacets of the non js version of the facet search
     * 
     * TODO The creation of subfacets urls has errors reported in DDBNEXT-974 and DDBNEXT-984
     * Since the role facets are not part of the 4.2 release these errors has only been solved for the subfacets.
     * 
     * @param rolefacets a list with all role facets
     * @param mainFacetsUrl a list with all mainFacetsUrl
     * @param subFacetsUrl a list with all subFacetsUrl
     * @param urlQuery the urlQuery
     * 
     * @return a list with all roleFacetsUrls
     */
    def buildRoleFacetsUrl(List rolefacets, LinkedHashMap mainFacetsUrl, LinkedHashMap subFacetsUrl, LinkedHashMap urlQuery){
        def res = []
        def allBackendRolefacets = getRoleFacets()

        rolefacets.each { rf->
            if(rf.numberOfFacets>0){
                rf.facetValues.each{ fv->

                    def roleFacetDefinition = allBackendRolefacets.find {
                        it.name = rf.field
                    }

                    def tmpFacetValuesMap = ["parent": roleFacetDefinition.parent, "field":rf.field, "fctValue": fv.value,"url":"",cnt: fv["count"],selected:""]
                    def mainUrl = mainFacetsUrl.find{
                        rf.field.contains(it.key)
                    }

                    def tmpUrl = mainUrl.value

                    //remove the facetvalue from the URL (the role facet is selected)
                    if(tmpUrl.contains(rf.field+"="+fv["value"])){
                        tmpUrl = tmpUrl.replaceAll("&facetValues%5B%5D="+rf.field+"="+fv["value"],"")
                        tmpFacetValuesMap["url"] = tmpUrl
                        tmpFacetValuesMap["selected"] = "selected"

                        //remove also the role facets from the corresponding subFacetUrl

                        subFacetsUrl.each {  key, value  ->
                            if (rf.field.contains(key)) {
                                value.each { subUrl ->
                                    if (subUrl.fctValue.equals(fv['value'])) {
                                        def query = "&facetValues%5B%5D="+rf.field+"="+fv["value"]

                                        //replace the url in the subUrl Map
                                        def cleanedSubUrl = subUrl.url.replaceAll(query,"")
                                        subUrl.url = cleanedSubUrl
                                    }
                                }
                            }
                        }
                    }
                    //add the value to the link (the role facet is deselected)
                    else{
                        tmpUrl += "&facetValues%5B%5D="+rf.field+"%3D"+fv["value"]
                        tmpFacetValuesMap["url"] = tmpUrl
                    }

                    res.add(tmpFacetValuesMap)
                }
            }
        }

        return res
    }


    /**
     * 
     * Build the list of facets to be rendered in the non javascript version of search results
     * 
     * @param urlQuery the urlQuery
     * @return list of all facets filtered
     */
    def buildSubFacets(LinkedHashMap urlQuery){
        def emptyFacets = this.facetsList.clone()
        def res = []
        //We want only the first 10 facets
        urlQuery["facet.limit"] = 10

        urlQuery[SearchParamEnum.FACET.getName()].each{
            if(it != "grid_preview"){
                emptyFacets.remove(it)
                def tmpUrlQuery = urlQuery.clone()
                tmpUrlQuery[SearchParamEnum.ROWS.getName()]=1
                tmpUrlQuery[SearchParamEnum.OFFSET.getName()]=0
                tmpUrlQuery.remove(it)
                def apiResponse = ApiConsumer.getJson(configurationService.getApisUrl() ,'/apis/search', false, tmpUrlQuery)
                if(!apiResponse.isOk()){
                    log.error "Json: Json file was not found"
                    apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
                }
                def jsonResp = apiResponse.getResponse()
                jsonResp.facets.each{ facet->
                    if(facet.field==it){
                        res.add(facet)
                    }
                }
            }
        }
        //fill the remaining empty facets
        emptyFacets.each{
            res.add([field: it, numberOfFacets: 0, facetValues: []])
        }
        return res
    }

    /**
     * Build the list of role facets to be rendered in the non javascript version of search results
     *
     * @param urlQuery the urlQuery
     * @return list of all facets filtered
     */
    def buildRoleFacets(LinkedHashMap urlQuery){
        def res = []
        def roleFacets = getRoleFacets()

        roleFacets.each { roleFacet ->
            if (urlQuery[roleFacet.parent] != null) {

                urlQuery[roleFacet.parent].each { facetValue ->
                    def searchUrl = '/search/facets/' + roleFacet.name

                    def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl() ,searchUrl , false, [query:facetValue])
                    if(!apiResponse.isOk()){
                        log.error "Json: Json file was not found"
                        apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
                    }
                    def jsonResp = apiResponse.getResponse()

                    if (jsonResp.numberOfFacets > 0) {
                        res.add(jsonResp)
                    }
                }
            }
        }

        return res
    }

    def buildPagination(int resultsNumber, LinkedHashMap queryParameters, String getQuery){
        def res = [firstPg:null,lastPg:null,prevPg:null,nextPg:null]
        //if resultsNumber greater rows number no buttons else we can start to create the pagination
        def currentRows = queryParameters[SearchParamEnum.ROWS.getName()].toInteger()
        def currentOffset = queryParameters[SearchParamEnum.OFFSET.getName()].toInteger()
        if(!getQuery.contains(SearchParamEnum.ROWS.getName()))
            getQuery += "&"+SearchParamEnum.ROWS.getName()+"=20"
        if(resultsNumber>currentRows){
            //We are not at the first page
            if(currentOffset>0){
                def prevUrl
                def firstUrl
                def offsetPrev = currentOffset - currentRows
                def offsetFirst = 0
                if(getQuery.contains(SearchParamEnum.OFFSET.getName())){
                    prevUrl = getQuery.replaceAll(SearchParamEnum.OFFSET.getName()+'='+currentOffset, SearchParamEnum.OFFSET.getName()+'='+offsetPrev)
                    firstUrl = getQuery.replaceAll(SearchParamEnum.OFFSET.getName()+'='+currentOffset, SearchParamEnum.OFFSET.getName()+'='+offsetFirst)
                }else{
                    prevUrl = getQuery+'&'+SearchParamEnum.OFFSET.getName()+'='+offsetPrev
                    firstUrl = getQuery+'&'+SearchParamEnum.OFFSET.getName()+'='+offsetFirst
                }
                res["firstPg"]= firstUrl
                res["prevPg"]= prevUrl
            }
            //We are not at the last page
            if(currentOffset+currentRows<resultsNumber){
                def offsetNext = currentOffset + currentRows
                def offsetLast = ((Math.ceil(resultsNumber/currentRows)*currentRows)-currentRows).toInteger()
                def nextUrl
                def lastUrl
                if(getQuery.contains(SearchParamEnum.OFFSET.getName())){
                    nextUrl = getQuery.replaceAll(SearchParamEnum.OFFSET.getName()+'='+currentOffset, SearchParamEnum.OFFSET.getName()+'='+offsetNext)
                    lastUrl = getQuery.replaceAll(SearchParamEnum.OFFSET.getName()+'='+currentOffset, SearchParamEnum.OFFSET.getName()+'='+offsetLast)
                }else{
                    nextUrl = getQuery+'&'+SearchParamEnum.OFFSET.getName()+'='+offsetNext
                    lastUrl = getQuery+'&'+SearchParamEnum.OFFSET.getName()+'='+offsetLast
                }
                res["lastPg"]= lastUrl
                res["nextPg"]= nextUrl
            }
        }
        return res
    }

    def buildPaginatorOptions(LinkedHashMap queryMap){
        def pageFilter = [10, 20, 40, 60, 100]
        if(!pageFilter.contains(queryMap[SearchParamEnum.ROWS.getName()].toInteger()))
            pageFilter.add(queryMap[SearchParamEnum.ROWS.getName()].toInteger())
        return [pageFilter: pageFilter.sort(), pageFilterSelected: queryMap[SearchParamEnum.ROWS.getName()].toInteger(), sortResultsSwitch: queryMap[SearchParamEnum.SORT.getName()]]
    }

    def buildClearFilter(LinkedHashMap urlQuery, String baseURI){
        def res = baseURI+'?'
        urlQuery.each{ key, value ->
            if(!key.toString().contains(SearchParamEnum.FACET.getName()) && !key.toString().contains(SearchParamEnum.FACETVALUES.getName()) && !key.toString().contains("fct")){
                res+='&'+key+'='+value
            }
        }
        return res
    }

    /**
     * 
     * Gives you back the HTML title with "strong" attributes trimmed to desired length
     * 
     * @param title
     * @param length
     * @return String title
     */
    def trimTitle(String title, int length){
        def matchesMatch = title =~ /(?m)<match>(.*?)<\/match>/
        def cleanTitle = title.replaceAll("<match>", "").replaceAll("</match>", "")
        def index = cleanTitle.length() > length ? cleanTitle.substring(0,length).lastIndexOf(" ") : -1
        def tmpTitle = index >= 0 ? cleanTitle.substring(0,index) + "..." : cleanTitle
        StringBuilder replacementsRegex = new StringBuilder("(")
        if(matchesMatch.size()>0){
            matchesMatch.each{
                if (replacementsRegex.size() > 1) {
                    replacementsRegex.append("|")
                }
                replacementsRegex.append(Pattern.quote(it[1]))
            }
            replacementsRegex.append(")")
            tmpTitle = tmpTitle.replaceAll(replacementsRegex.toString(), '<strong>$1</strong>')
        }
        return tmpTitle
    }

    /**
     *
     * Gives you back the string trimmed to desired length
     *
     * @param text
     * @param length
     * @return String text trimmed
     */
    def trimString(String text, int length){
        if(text.length()>length)
            return text.substring(0, text.substring(0,length).lastIndexOf(" "))+"..."
        return text
    }

    /**
     * Generate Map that can be used to call Search on Search-Server
     * 
     * @param reqParameters
     * @return Map with keys used for Search on Search-Server
     */
    def convertQueryParametersToSearchParameters(Map reqParameters) {
        def urlQuery = [:]
        def numbersRangeRegex = /^[0-9]+$/

        if (reqParameters[SearchParamEnum.QUERY.getName()]!=null && reqParameters[SearchParamEnum.QUERY.getName()].length()>0){
            urlQuery[SearchParamEnum.QUERY.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.QUERY.getName(), "*")
        }else{
            urlQuery[SearchParamEnum.QUERY.getName()] = "*"
        }

        if (reqParameters[SearchParamEnum.ROWS.getName()] == null || !(reqParameters[SearchParamEnum.ROWS.getName()]=~ numbersRangeRegex)) {
            urlQuery[SearchParamEnum.ROWS.getName()] = 20.toInteger()
        } else {
            urlQuery[SearchParamEnum.ROWS.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.ROWS.getName(), "20").toInteger()
        }
        reqParameters[SearchParamEnum.ROWS.getName()] = urlQuery[SearchParamEnum.ROWS.getName()]

        if (reqParameters[SearchParamEnum.OFFSET.getName()] == null || !(reqParameters[SearchParamEnum.OFFSET.getName()]=~ numbersRangeRegex)) {
            urlQuery[SearchParamEnum.OFFSET.getName()] = 0.toInteger()
        } else {
            urlQuery[SearchParamEnum.OFFSET.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.OFFSET.getName(), "0").toInteger()
        }
        reqParameters[SearchParamEnum.OFFSET.getName()] = urlQuery[SearchParamEnum.OFFSET.getName()]

        //<--input query=rom&offset=0&rows=20&facetValues%5B%5D=time_fct%3Dtime_61800&facetValues%5B%5D=time_fct%3Dtime_60100&facetValues%5B%5D=place_fct%3DItalien
        //-->output query=rom&offset=0&rows=20&facet=time_fct&time_fct=time_61800&facet=time_fct&time_fct=time_60100&facet=place_fct&place_fct=Italien
        if(reqParameters[SearchParamEnum.FACETVALUES.getName()]){
            urlQuery = this.getFacets(reqParameters, urlQuery,SearchParamEnum.FACET.getName(), 0)
        }

        if(reqParameters.get(SearchParamEnum.FACETS.getName())){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains(reqParameters.get(SearchParamEnum.FACETS.getName())))
                urlQuery[SearchParamEnum.FACET.getName()].add(reqParameters.get(SearchParamEnum.FACETS.getName()))
        }

        if(reqParameters[SearchParamEnum.MINDOCS.getName()]) {
            urlQuery[SearchParamEnum.MINDOCS.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.MINDOCS.getName(), "")
        }

        if(reqParameters[SearchParamEnum.SORT.getName()] != null && ((reqParameters[SearchParamEnum.SORT.getName()]=~ /^random_[0-9]+$/) || reqParameters[SearchParamEnum.SORT.getName()]==SearchParamEnum.SORT_ALPHA_ASC.getName() || reqParameters[SearchParamEnum.SORT.getName()]==SearchParamEnum.SORT_ALPHA_DESC.getName())){
            urlQuery[SearchParamEnum.SORT.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.SORT.getName(), "")
        }else{
            if(urlQuery[SearchParamEnum.QUERY.getName()]!="*"){
                urlQuery[SearchParamEnum.SORT.getName()] = SearchParamEnum.SORT_RELEVANCE.getName()
            }
        }

        if(reqParameters[SearchParamEnum.VIEWTYPE.getName()] == null || (!(reqParameters[SearchParamEnum.VIEWTYPE.getName()]=~ /^list$/) && !(reqParameters[SearchParamEnum.VIEWTYPE.getName()]=~ /^grid$/))) {
            urlQuery[SearchParamEnum.VIEWTYPE.getName()] = SearchParamEnum.VIEWTYPE_LIST.getName()
            reqParameters[SearchParamEnum.VIEWTYPE.getName()] = SearchParamEnum.VIEWTYPE_LIST.getName()
        } else {
            urlQuery[SearchParamEnum.VIEWTYPE.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.VIEWTYPE.getName(), "")
        }

        if(reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()]){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains("grid_preview") && reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()] == "true"){
                urlQuery[SearchParamEnum.FACET.getName()].add("grid_preview")
                urlQuery["grid_preview"] = "true"
            }
        }

        if(!urlQuery[SearchParamEnum.FACET.getName()]){
            urlQuery[SearchParamEnum.FACET.getName()] = []
        }

        // This is needed for the entity search results that are displayed on top of the regular search results.
        urlQuery[SearchParamEnum.FACET.getName()].add(FacetEnum.AFFILIATE_INVOLVED_NORMDATA.getName())
        urlQuery[SearchParamEnum.FACET.getName()].add(FacetEnum.AFFILIATE_SUBJECT.getName())

        return urlQuery
    }

    /**
     * Generate Map that can be used to call Autocomplete and Search Facets on Search-Server
     *
     * @param reqParameters
     * @return Map with keys used for Search on Search-Server
     */
    def convertQueryParametersToSearchFacetsParameters(Map reqParameters) {
        def urlQuery = [:]

        if (reqParameters["searchQuery"]!=null && reqParameters["searchQuery"].length()>0){
            urlQuery["searchQuery"] = getMapElementOfUnsureType(reqParameters, "searchQuery", "*")
        }else{
            urlQuery["searchQuery"] = "*"
        }

        if (reqParameters[SearchParamEnum.QUERY.getName()]!=null && reqParameters[SearchParamEnum.QUERY.getName()].length()>0){
            urlQuery[SearchParamEnum.QUERY.getName()] = getMapElementOfUnsureType(reqParameters, SearchParamEnum.QUERY.getName(), "*")
        }else{
            urlQuery[SearchParamEnum.QUERY.getName()] = "*"
        }

        //<--input query=rom&offset=0&rows=20&facetValues%5B%5D=time_fct%3Dtime_61800&facetValues%5B%5D=time_fct%3Dtime_60100&facetValues%5B%5D=place_fct%3DItalien
        //-->output query=rom&offset=0&rows=20&facet=time_fct&time_fct=time_61800&facet=time_fct&time_fct=time_60100&facet=place_fct&place_fct=Italien
        if(reqParameters[SearchParamEnum.FACETVALUES.getName()]){
            urlQuery = this.getFacets(reqParameters, urlQuery,SearchParamEnum.FACET.getName(), 0)
        }

        if(reqParameters.get(SearchParamEnum.FACETS.getName())){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains(reqParameters.get(SearchParamEnum.FACETS.getName())))
                urlQuery[SearchParamEnum.FACET.getName()].add(reqParameters.get(SearchParamEnum.FACETS.getName()))
        }

        if(reqParameters["sortDesc"] != null && ((reqParameters["sortDesc"]== "true") || (reqParameters["sortDesc"]== "false"))){
            urlQuery["sortDesc"] = getMapElementOfUnsureType(reqParameters, "sortDesc", "")
        }

        if(reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()]){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains("grid_preview") && reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()] == "true"){
                urlQuery[SearchParamEnum.FACET.getName()].add("grid_preview")
                urlQuery["grid_preview"] = "true"
            }
        }

        //We ask for a maximum of 301 facets
        urlQuery["facet.limit"] = CortexConstants.MAX_FACET_SEARCH_RESULTS

        return urlQuery
    }

    /**
     * Utility-method to fix a groovy-inconvenience. Parameter map values can either be a single String or
     * an Array of Strings (e.g. if the parameter was defined twice in the URL). To handle this, get the 
     * parameters over this method.
     * @param map The parameter map
     * @param elementName The map key
     * @param defaultValue The default value if no value was found for the key
     * @return The value or the defaultValue if no value was found
     */
    private String getMapElementOfUnsureType(map, elementName, defaultValue){
        if (map[elementName]?.class.isArray()){
            if(map[elementName].size() > 0){
                return map[elementName][0]
            } else {
                return defaultValue
            }
        }else{
            if(map[elementName]){
                return map[elementName]
            } else {
                return defaultValue
            }
        }

    }

    /**
     * Generate Map that contains GET-parameters used for search-request by ddb-next.
     * 
     * @param reqParameters
     * @return Map with keys used for search-request by ddb-next.
     */
    def getSearchGetParameters(Map reqParameters) {
        def searchParams = [:]
        def requiredParams = [
            SearchParamEnum.QUERY.getName(),
            SearchParamEnum.OFFSET.getName(),
            SearchParamEnum.ROWS.getName(),
            SearchParamEnum.SORT.getName(),
            SearchParamEnum.VIEWTYPE.getName(),
            SearchParamEnum.CLUSTERED.getName(),
            SearchParamEnum.IS_THUMBNAILS_FILTERED.getName(),
            SearchParamEnum.FACETVALUES.getName(),
            SearchParamEnum.FACETS.getName()
        ]
        for (entry in reqParameters) {
            if (requiredParams.contains(entry.key)) {
                searchParams[entry.key] = entry.value
            }
        }
        return searchParams
    }

    /**
     * Generate Map that contains GET-parameters used for item-detail-request by ddb-next.
     * 
     * @param reqParameters
     * @return Map with keys used for item-detail-request by ddb-next.
     */
    def getSearchCookieParameters(Map reqParameters) {
        def searchCookieParameters = [:]
        def requiredParams = [
            SearchParamEnum.QUERY.getName(),
            SearchParamEnum.OFFSET.getName(),
            SearchParamEnum.ROWS.getName(),
            SearchParamEnum.SORT.getName(),
            SearchParamEnum.VIEWTYPE.getName(),
            SearchParamEnum.CLUSTERED.getName(),
            SearchParamEnum.IS_THUMBNAILS_FILTERED.getName(),
            SearchParamEnum.FACETVALUES.getName(),
            SearchParamEnum.FACETS.getName(),
            SearchParamEnum.FIRSTHIT.getName(),
            SearchParamEnum.LASTHIT.getName(),
            SearchParamEnum.KEEPFILTERS.getName()
        ]
        for (entry in reqParameters) {
            if (requiredParams.contains(entry.key)) {
                searchCookieParameters[entry.key] = entry.value
            }
        }
        return searchCookieParameters
    }

    /**
     * 
     * Used in FacetsController gives you back an array containing the following Map: {facet value, localized facet value, count results} 
     * 
     * @param facets list of facets fetched from the backend
     * @param fctName name of the facet field required
     * @param numberOfElements number of elements to return
     * @return List of Map
     */
    def getSelectedFacetValues(net.sf.json.JSONObject facets, String fctName, int numberOfElements, String matcher, Locale locale){
        def res = [type: fctName, values: []]
        def allFacetFilters = configurationService.getFacetsFilter()

        int max = (numberOfElements != -1 && facets.numberOfFacets>numberOfElements)?numberOfElements:facets.numberOfFacets
        for(int i=0;i<max;i++){

            //Check if facet value has to be filtered
            boolean filterFacet = false
            for(int k=0; k<allFacetFilters.size(); k++){
                if(fctName == allFacetFilters[k].facetName && facets.facetValues[i].value.toString() == allFacetFilters[k].filter){
                    filterFacet = true
                    break
                }
            }

            if(!filterFacet){
                if(matcher && facets.facetValues[i].value.toString().toLowerCase().contains(matcher.toLowerCase())){
                    def facetValue = facets.facetValues[i].value
                    def firstIndexMatcher = facetValue.toLowerCase().indexOf(matcher.toLowerCase())
                    facetValue = facetValue.substring(0, firstIndexMatcher)+"<strong>"+facetValue.substring(firstIndexMatcher,firstIndexMatcher+matcher.size())+"</strong>"+facetValue.substring(firstIndexMatcher+matcher.size(),facetValue.size())
                    res.values.add([value: facets.facetValues[i].value, localizedValue: facetValue, count: String.format(locale, "%,d", facets.facetValues[i].count.toInteger())])
                } else {
                    res.values.add([value: facets.facetValues[i].value, localizedValue: this.getI18nFacetValue(fctName, facets.facetValues[i].value.toString()), count: String.format(locale, "%,d", facets.facetValues[i].count.toInteger())])
                }
            }
        }
        return res
    }

    /**
     *
     * Used in FacetsController gives you back an array containing the following Map: {facet value, localized facet value, count results}
     *
     * @param facets list of facets fetched from the backend
     * @param fctName name of the facet field required
     * @param numberOfElements number of elements to return
     * @return List of Map
     */
    def getSelectedFacetValuesFromOldApi(List facets, String fctName, int numberOfElements, String matcher, Locale locale){
        def res = [type: fctName, values: []]
        def allFacetFilters = grailsApplication.config.ddb.backend.facets.filter

        facets.each{
            if(it.field==fctName){
                int max = (numberOfElements != -1 && it.facetValues.size()>numberOfElements)?numberOfElements:it.facetValues.size()
                for(int i=0;i<max;i++){
                    //Check if facet value has to be filtered
                    boolean filterFacet = false
                    for(int k=0; k<allFacetFilters.size(); k++){
                        if(fctName == allFacetFilters[k].facetName && it.facetValues[i].value.toString() == allFacetFilters[k].filter){
                            filterFacet = true
                            break
                        }
                    }

                    if(!filterFacet){
                        if(matcher && this.getI18nFacetValue(fctName, it.facetValues[i].value.toString()).toLowerCase().contains(matcher.toLowerCase())){
                            def localizedValue = this.getI18nFacetValue(fctName, it.facetValues[i].value.toString())
                            def firstIndexMatcher = localizedValue.toLowerCase().indexOf(matcher.toLowerCase())
                            localizedValue = localizedValue.substring(0, firstIndexMatcher)+"<strong>"+localizedValue.substring(firstIndexMatcher,firstIndexMatcher+matcher.size())+"</strong>"+localizedValue.substring(firstIndexMatcher+matcher.size(),localizedValue.size())
                            res.values.add([value: it.facetValues[i].value, localizedValue: localizedValue, count: String.format(locale, "%,d", it.facetValues[i].count.toInteger())])
                        }else if(!matcher)
                            res.values.add([value: it.facetValues[i].value, localizedValue: this.getI18nFacetValue(fctName, it.facetValues[i].value.toString()), count: String.format(locale, "%,d", it.facetValues[i].count.toInteger())])
                    }
                }
            }
        }
        return res
    }

    /**
     * 
     * Gives you back the passed facet value internationalized
     * 
     * @param facetName
     * @param facetValue
     * @return String i18n facet value
     */
    def getI18nFacetValue(facetName, facetValue){

        def appCtx = grailsApplication.getMainContext()

        def res = ""

        if(facetName == FacetEnum.AFFILIATE.getName() || facetName == FacetEnum.KEYWORDS.getName() || facetName == FacetEnum.PLACE.getName() || facetName == FacetEnum.PROVIDER.getName()){
            res = facetValue
        }
        else if(facetName == FacetEnum.TYPE.getName()){
            res = appCtx.getMessage(FacetEnum.TYPE.getI18nPrefix()+facetValue, null, LocaleContextHolder.getLocale() )
        }
        else if(facetName == FacetEnum.TIME.getName()){
            res = appCtx.getMessage(FacetEnum.TIME.getI18nPrefix()+facetValue, null, LocaleContextHolder.getLocale())
        }
        else if(facetName == FacetEnum.LANGUAGE.getName()){
            res = appCtx.getMessage(FacetEnum.LANGUAGE.getI18nPrefix()+facetValue, null, LocaleContextHolder.getLocale())
        }
        else if(facetName == FacetEnum.SECTOR.getName()){
            res = appCtx.getMessage(FacetEnum.SECTOR.getI18nPrefix()+facetValue, null, LocaleContextHolder.getLocale())
        }
        return res
    }

    /**
     * Create Cookie with search-parameters for use on other pages
     * convert HashMap containing parameters to JSON
     * 
     * @param reqParameters request-parameters
     * @return Cookie with search-parameters
     */
    def createSearchCookie( HttpServletRequest requestObject, Map reqParameters, Map additionalParams) {
        //Create Cookie with search-parameters for use on other pages
        //convert HashMap containing parameters to JSON
        if (additionalParams) {
            for (entry in additionalParams) {
                reqParameters[entry.key] = entry.value
            }
        }
        Map paramMap = getSearchCookieParameters(reqParameters)
        def jSonObject = new JSONObject()
        for (entry in paramMap) {
            if (entry.value instanceof String[]) {
                for (entry1 in entry.value) {
                    jSonObject.accumulate(entry.key, URLEncoder.encode(entry1, CHARACTER_ENCODING))
                }
            }
            else if (entry.value instanceof String){
                jSonObject.put(entry.key, URLEncoder.encode(entry.value, CHARACTER_ENCODING))
            }
            else {
                jSonObject.put(entry.key, entry.value)
            }
        }
        def cookie = new Cookie(searchCookieName + requestObject.contextPath, jSonObject.toString())
        cookie.maxAge = -1
        return cookie
    }

    /**
     * Reads the cookie containing the search-Parameters and fills the values in Map.
     * 
     * @param request
     * @return Map with key-values from cookie
     */
    def getSearchCookieAsMap(HttpServletRequest requestObject, Cookie[] cookies) {
        def searchParams
        def searchParamsMap = [:]
        for (cookie in cookies) {
            if (cookie.name == searchCookieName + requestObject.contextPath) {
                searchParams = cookie.value
            }
        }
        if (searchParams) {
            def jSonSlurper = new JsonSlurper()
            try{
                searchParamsMap = jSonSlurper.parseText(searchParams)
            }catch(Exception e){
                log.error "getSearchCookieAsMap(): Could not parse search params: "+searchParams, e
            }
            for (entry in searchParamsMap) {
                if (entry.value instanceof String) {
                    entry.value = URLDecoder.decode(entry.value, CHARACTER_ENCODING)
                }
                else if (entry.value instanceof List) {
                    String[] arr = new String[entry.value.size()]
                    def i = 0
                    for (entry1 in entry.value) {
                        if (entry1 instanceof String) {
                            entry1 = URLDecoder.decode(entry1, CHARACTER_ENCODING)
                        }
                        arr[i] = entry1
                        i++
                    }
                    entry.value = arr
                }
            }
        }
        return searchParamsMap
    }

    /**
     * Converts the params list received from the frontend during a request to get all the facets to be displayed in the flyout widget.
     * 
     * @param reqParameters the params variable containing all the req parameters
     * @return a map containing all the converted request parameters ready to be submitted to the related service to fetch the right facets values
     */
    def convertFacetQueryParametersToFacetSearchParameters(Map reqParameters) {
        def urlQuery = [:]

        if (reqParameters.searchQuery == null)
            urlQuery[SearchParamEnum.QUERY.getName()] = '*'
        else urlQuery[SearchParamEnum.QUERY.getName()] = reqParameters.searchQuery

        if (reqParameters[SearchParamEnum.ROWS.getName()] == null || reqParameters[SearchParamEnum.ROWS.getName()] == -1)
            urlQuery[SearchParamEnum.ROWS.getName()] = 1
        else urlQuery[SearchParamEnum.ROWS.getName()] = reqParameters[SearchParamEnum.ROWS.getName()]

        if (reqParameters[SearchParamEnum.OFFSET.getName()] == null)
            urlQuery[SearchParamEnum.OFFSET.getName()] = 0
        else urlQuery[SearchParamEnum.OFFSET.getName()] = reqParameters[SearchParamEnum.OFFSET.getName()]

        //<--input query=rom&offset=0&rows=20&facetValues%5B%5D=time_fct%3Dtime_61800&facetValues%5B%5D=time_fct%3Dtime_60100&facetValues%5B%5D=place_fct%3DItalien
        //-->output query=rom&offset=0&rows=20&facet=time_fct&time_fct=time_61800&facet=time_fct&time_fct=time_60100&facet=place_fct&place_fct=Italien
        if(reqParameters[SearchParamEnum.FACETVALUES.getName()]){
            urlQuery = getFacets(reqParameters, urlQuery,SearchParamEnum.FACET.getName(), 0)
        }

        if(reqParameters.get("name")){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains(reqParameters.get("name")))
                urlQuery[SearchParamEnum.FACET.getName()].add(reqParameters.get("name"))
        }


        if(reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()]){
            urlQuery[SearchParamEnum.FACET.getName()] = (!urlQuery[SearchParamEnum.FACET.getName()])?[]:urlQuery[SearchParamEnum.FACET.getName()]
            if(!urlQuery[SearchParamEnum.FACET.getName()].contains("grid_preview") && reqParameters[SearchParamEnum.IS_THUMBNAILS_FILTERED.getName()] == "true"){
                urlQuery[SearchParamEnum.FACET.getName()].add("grid_preview")
                urlQuery["grid_preview"] = "true"
            }
        }

        //We ask for a maximum of 1000 facets
        urlQuery["facet.limit"] = 1000

        return urlQuery
    }

    /**
     * Check if searchCookie contains keepFilters=true.
     * If yes, expand requestParameters with facets and return true.
     * Otherwise return false
     * 
     * @param cookieMap
     * @param requestParameters
     * @return boolean
     */
    def checkPersistentFacets(Map cookieMap, Map requestParameters, Map additionalParams) {
        if (cookieMap[SearchParamEnum.KEEPFILTERS.getName()] && cookieMap[SearchParamEnum.KEEPFILTERS.getName()] == "true") {
            additionalParams[SearchParamEnum.KEEPFILTERS.getName()] = "true"
            if (!requestParameters[SearchParamEnum.FACETVALUES.getName()] && cookieMap[SearchParamEnum.FACETVALUES.getName()]) {
                requestParameters[SearchParamEnum.FACETVALUES.getName()] = cookieMap[SearchParamEnum.FACETVALUES.getName()]
                return true
            }
            else {
                return false
            }
        }
        else {
            return false
        }
    }

    def checkAndReplaceMediaTypeImages(def searchResult){
        searchResult.results.docs.each {
            def preview = it.preview
            if(preview.thumbnail == null ||
            preview.thumbnail instanceof net.sf.json.JSONNull ||
            preview.thumbnail.toString().trim().isEmpty() ||
            (preview.thumbnail.toString().startsWith("http://content") &&
            preview.thumbnail.toString().contains("/placeholder/searchResult"))
            ){
                def mediaTypes = []
                if(preview.media instanceof String){
                    mediaTypes.add(preview.media)
                }else{
                    mediaTypes.addAll(preview.media)
                }
                def mediaType = mediaTypes[0]
                if(mediaType != null){
                    mediaType = mediaType.toString().toLowerCase().capitalize()
                }
                if(mediaType != "Audio" &&
                mediaType != "Image" &&
                mediaType != "Institution" &&
                mediaType != "Sonstiges" &&
                mediaType != "Text" &&
                mediaType != "Unknown" &&
                mediaType != "Video"){
                    mediaType = "Unknown"
                }
                def g = grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
                preview.thumbnail = g.resource("dir": "images", "file": "/placeholder/searchResultMedia"+mediaType+".png").toString()
            }
        }
        return searchResult
    }

    /**
     * Returns all role facets from the backend.
     * The method requests all available facets and then filter for the role attribute.
     * 
     * A role facet looks like this:
     * [name:affiliate_fct_involved, parent:affiliate_fct, paths:[], role:involved, searchType:TEXT, sortType:null, displayType:TECHNICAL, position:-1]
     * 
     * @return a list of all role facets in the json format
     */
    def getRoleFacets() {
        def res = []

        def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl(),'/search/facets/')
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(request)
        }

        def resultsItems = apiResponse.getResponse()
        resultsItems.each {
            if (it.role != 'null') {
                //FIXME set sortType '' to avoid "net.sf.json.JSONException: Object is null" exception
                it.sortType = ''
                res.add(it)
            }
        }
        return res
    }

}
