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
import static groovyx.net.http.Method.*

import java.util.regex.Matcher
import java.util.regex.Pattern

import net.sf.json.JSONNull

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.context.NoSuchMessageException
import org.springframework.web.servlet.support.RequestContextUtils

import de.ddb.common.beans.Bookmark
import de.ddb.common.beans.User
import de.ddb.common.constants.CortexNamespace
import de.ddb.common.constants.SearchParamEnum
import de.ddb.common.constants.SupportedLocales
import de.ddb.common.constants.Type
import de.ddb.common.exception.ItemNotFoundException

class ItemService {
    private static final log = LogFactory.getLog(this)

    private static final def HTTP ='http://'
    private static final def HTTPS ='https://'

    private static final SOURCE_PLACEHOLDER = '{0}'
    private static final def THUMBNAIL = 'mvth'
    private static final def PREVIEW= 'mvpr'
    private static final def FULL = 'full'
    private static final def ORIG= 'orig'
    private static final def IMAGE= 'image/jpeg'
    private static final def AUDIO = 'audio/mp3'
    private static final def VIDEOMP4 = 'video/mp4'
    private static final def VIDEOFLV = 'video/flv'

    private static final def MAX_LENGTH_FOR_ITEM_WITH_BINARY = 270
    private static final def MAX_LENGTH_FOR_ITEM_WITH_NO_BINARY = 350

    def transactional = false
    def grailsApplication
    def configurationService
    def searchService
    def messageSource
    def sessionService
    def cultureGraphService
    def bookmarksService

    LinkGenerator grailsLinkGenerator

    def findItemById(id) {

        final def componentsPath = "/items/" + id + "/"
        final def viewPath = componentsPath + "view"

        def apiResponse = ApiConsumer.getXml(configurationService.getBackendUrl(), viewPath)
        if(!apiResponse.isOk()){
            log.error "findItemById: xml file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        def xml = apiResponse.getResponse()

        def ns2Namespace = xml.lookupNamespace(CortexNamespace.NS2.prefix)
        if(ns2Namespace == CortexNamespace.RDF.uri){
            log.error "findItemById(): Cortex returned namespace ns2 instead of rdf for item "+id
        }

        //def institution= xml.institution
        def institution= xml.item.institution

        String institutionLogoUrl = grailsLinkGenerator.resource("dir": "images", "file": "/placeholder/searchResultMediaInstitution.png").toString()
        if(xml.item.institution.logo != null && !xml.item.institution.logo.toString().trim().isEmpty()){
            institutionLogoUrl = filterOutSurroundingTag(xml.item.institution.logo.toString())
        }

        String originUrl = filterOutSurroundingTag(xml.item.origin.toString())

        def item = xml.item

        def title = shortenTitle(id, item)

        def displayFieldsTag = xml.item.fields.findAll{ it.@usage.text().contains('display') }
        def fields = displayFieldsTag[0].field.findAll()

        def viewerUri = buildViewerUri(item, componentsPath)

        return ['uri': '', 'viewerUri': viewerUri, 'institution': institution, 'item': item, 'title': title,
            'fields': fields, pageLabel: xml.pagelabel, 'institutionImage': institutionLogoUrl, 'originUrl': originUrl]
    }


    def getFullItemModel(id) {
        def utils = WebUtils.retrieveGrailsWebRequest()
        def request = utils.getCurrentRequest()
        def response = utils.getCurrentResponse()
        def params = utils.getParameterMap()

        //Check if Item-Detail was called from search-result and fill parameters
        def searchResultParameters = handleSearchResultParameters(params, request)

        def item = findItemById(id)

        if("404".equals(item)){
            throw new ItemNotFoundException()
        }

        def isFavorite = isFavorite(id)
        log.info("params.reqActn = ${params.reqActn} --> " + params.reqActn)
        if (params.reqActn) {
            if (params.reqActn.equalsIgnoreCase("add") && (isFavorite == response.SC_NOT_FOUND) && addFavorite(id)) {
                isFavorite = response.SC_FOUND
            }
            else if (params.reqActn.equalsIgnoreCase("del") && (isFavorite == response.SC_FOUND) && delFavorite(id)) {
                isFavorite = response.SC_NOT_FOUND
            }
        }

        def binaryList = findBinariesById(id)
        def binariesCounter = binariesCounter(binaryList)

        def flashInformation = [:]
        flashInformation.all = [binaryList.size]
        flashInformation.images = [binariesCounter.images]
        flashInformation.audios = [binariesCounter.audios]
        flashInformation.videos = [binariesCounter.videos]

        if (item.pageLabel?.isEmpty()) {
            item.pageLabel = item.title
        }

        def licenseInformation = buildLicenseInformation(item, request)

        def itemUri = request.forwardURI
        def fields = translate(item.fields, convertToHtmlLink, request)

        if(configurationService.isCulturegraphFeaturesEnabled()){
            fields = createEntityLinks(fields)
        }

        def model = [
            itemUri: itemUri,
            viewerUri: item.viewerUri,
            title: item.title,
            item: item.item,
            itemId: id,
            institution: item.institution,
            institutionImage: item.institutionImage,
            originUrl: item.originUrl,
            fields: fields,
            binaryList: binaryList,
            pageLabel: item.pageLabel,
            firstHit: searchResultParameters["searchParametersMap"][SearchParamEnum.FIRSTHIT.getName()],
            lastHit: searchResultParameters["searchParametersMap"][SearchParamEnum.LASTHIT.getName()],
            hitNumber: params["hitNumber"],
            results: searchResultParameters["resultsItems"],
            searchResultUri: searchResultParameters["searchResultUri"],
            flashInformation: flashInformation,
            license: licenseInformation,
            isFavorite: isFavorite,
            baseUrl: configurationService.getSelfBaseUrl()
        ]

        return model
    }


    private shortenTitle(id, item) {

        def title = item.title

        def hasBinary = !fetchBinaryList(id).isEmpty()

        if(title.size() <= MAX_LENGTH_FOR_ITEM_WITH_NO_BINARY) {
            return title
        }

        if(hasBinary && title.size() > MAX_LENGTH_FOR_ITEM_WITH_BINARY) {
            return apendDotDot(title.substring(0, MAX_LENGTH_FOR_ITEM_WITH_BINARY))
        } else if(title.size() > MAX_LENGTH_FOR_ITEM_WITH_NO_BINARY) {
            return apendDotDot(title.substring(0, MAX_LENGTH_FOR_ITEM_WITH_NO_BINARY))
        }

        return title
    }

    def apendDotDot(String shortenedTitle){
        def lastSpaceIndex = shortenedTitle.lastIndexOf(' ')
        def shortenedTitleUntilLastSpace  = shortenedTitle.substring(0, lastSpaceIndex)
        shortenedTitleUntilLastSpace + '...'
    }


    private def buildViewerUri(item, componentsPath) {
        if(item.viewers instanceof JSONNull){
            return ''
        }
        if(item.viewers?.viewer == null || item.viewers?.viewer?.isEmpty()) {
            return ''
        }

        def viewerPrefix = item.viewers.viewer.url.toString()

        if(viewerPrefix.contains(SOURCE_PLACEHOLDER)) {
            def withoutPlaceholder = viewerPrefix.toString() - SOURCE_PLACEHOLDER
            def binaryServerUrl = configurationService.getBinaryUrl()

            //Security check: if the binaryServerUrl is configured with an ending ".../binary/", this has to be removed
            int firstOccuranceOfBinaryString = binaryServerUrl.indexOf("/binary/")
            if(firstOccuranceOfBinaryString >= 0){
                binaryServerUrl = binaryServerUrl.substring(0, firstOccuranceOfBinaryString)
            }

            def sourceUri = binaryServerUrl + componentsPath + 'source'
            def encodedSourceUri= URLEncoder.encode sourceUri, 'UTF-8'
            return withoutPlaceholder + encodedSourceUri
        }
    }

    def findBinariesById(id) {
        def prev = parse(fetchBinaryList(id))
        return prev
    }

    private def fetchBinaryList(id) {
        def result = []
        def apiResponse = ApiConsumer.getXml(configurationService.getBackendUrl(), "/items/" + id + "/binaries")
        if (apiResponse.isOk()) {
            def binaries = apiResponse.getResponse()
            result = binaries.binary.list()
        }
        else if (apiResponse.status != ApiResponse.HttpStatus.HTTP_404) {
            log.error "fetchBinaryList: XML file could not be fetched"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        return result
    }

    private def parse(binaries) {
        def BINARY_SERVER_URI = grailsLinkGenerator.getContextPath()
        def binaryList = []
        def bidimensionalList = []
        String position
        String path
        String type
        String htmlStrip
        //creation of a bi-dimensional list containing the binaries separated for position
        binaries.each { x ->
            if(x.'@position'.toString() != position){
                def subList = []
                bidimensionalList[x.'@position'.toInteger()-1] = subList
                position = x.'@position'.toString()
            }
            bidimensionalList[x.'@position'.toInteger()-1].add(x)
        }
        //creation of a list of binary maps from the bi-dimensional list
        bidimensionalList.each { y ->
            def binaryMap = ['orig' : ['title':'', 'uri': ['image':'','audio':'','video':''],'author':'', 'rights':''],
                'preview' : ['title':'', 'uri':'', 'author':'', 'rights':''],
                'thumbnail' : ['title':'', 'uri':'','author':'', 'rights':''],
                'full' : ['title':'', 'uri':'','author':'', 'rights':''],
                'checkValue' : "",
            ]
            y.each { z ->
                path = z.'@path'
                type = z.'@mimetype'

                if(path.contains(ORIG)){
                    if(type.contains(IMAGE)){
                        binaryMap.'orig'.'uri'.'image' = BINARY_SERVER_URI + z.'@path'
                        if(!binaryMap.'orig'.'title') {
                            htmlStrip = z.'@name'
                            binaryMap.'orig'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                        }
                    }
                    else if(type.contains(AUDIO)){
                        binaryMap.'orig'.'uri'.'audio' = BINARY_SERVER_URI + z.'@path'
                        htmlStrip = z.'@name'
                        binaryMap.'orig'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                    }
                    else if(type.contains(VIDEOMP4)||type.contains(VIDEOFLV)){
                        binaryMap.'orig'.'uri'.'video' = BINARY_SERVER_URI + z.'@path'
                        htmlStrip = z.'@name'
                        binaryMap.'orig'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                    }

                    binaryMap.'orig'.'author'= z.'@name2'
                    binaryMap.'orig'.'rights'= z.'@name3'
                    binaryMap.'checkValue' = "1"
                }
                else if(path.contains(PREVIEW)) {
                    htmlStrip = z.'@name'
                    binaryMap.'preview'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                    binaryMap.'preview'.'uri' = BINARY_SERVER_URI + z.'@path'
                    binaryMap.'preview'.'author'= z.'@name2'
                    binaryMap.'preview'.'rights'= z.'@name3'
                    binaryMap.'checkValue' = "1"
                } else if (path.contains(THUMBNAIL)) {
                    htmlStrip = z.'@name'
                    binaryMap.'thumbnail'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                    binaryMap.'thumbnail'.'uri' = BINARY_SERVER_URI + z.'@path'
                    binaryMap.'thumbnail'.'author'= z.'@name2'
                    binaryMap.'thumbnail'.'rights'= z.'@name3'
                    binaryMap.'checkValue' = "1"
                } else if (path.contains(FULL)) {
                    htmlStrip = z.'@name'
                    binaryMap.'full'.'title' = htmlStrip.replaceAll("<(.|\n)*?>", '')
                    binaryMap.'full'.'uri' = BINARY_SERVER_URI + z.'@path'
                    binaryMap.'full'.'author'= z.'@name2'
                    binaryMap.'full'.'rights'= z.'@name3'
                    binaryMap.'checkValue' = "1"
                }
            }
            if(binaryMap.'checkValue'){
                binaryList.add(binaryMap)
            }
        }
        return binaryList
    }

    def binariesCounter(binaries){
        def images = 0
        def audios = 0
        def videos = 0
        binaries.each {
            if(it.'orig'.'uri'.'audio' || it.'orig'.'uri'.'video'){
                if(it.'orig'.'uri'.'audio'){
                    audios++
                }
                if(it.'orig'.'uri'.'video'){
                    videos++
                }
            } else if (it.'full'.'uri'){
                images++
            }
        }
        return (['images':images,'audios':audios,'videos':videos])
    }


    def getParent(itemId){
        final def parentsPath = "/hierarchy/" + itemId + "/parent/"
        def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl(), parentsPath)
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        return apiResponse.getResponse()
    }

    def getChildren(itemId){
        final def childrenPath = "/hierarchy/" + itemId + "/children/"
        def apiResponse = ApiConsumer.getJson(configurationService.getBackendUrl(), childrenPath)
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        return apiResponse.getResponse()
    }

    def fetchXMLMetadata(id) {
        def result = []
        def apiResponse = ApiConsumer.getXml(configurationService.getBackendUrl(), "/items/" + id + "/aip")
        if (apiResponse.isOk()) {
            result = apiResponse.getResponse().toXmlString()
        }
        else if (apiResponse.status != ApiResponse.HttpStatus.HTTP_404) {
            log.error "XMLMetadata: XML file could not be fetched"
            apiResponse.throwException(WebUtils.retrieveGrailsWebRequest().getCurrentRequest())
        }
        return result
    }

    private def log(list) {
        list.each { it ->
            log.debug "---"
            log.debug "name: ${it.'@name'}"
            log.debug "mime: ${it.'@mimetype'}"
            log.debug "path: ${it.'@path'}"
            log.debug "pos: ${it.'@position'}"
            log.debug "is primary?: ${it.'@primary'}"
        }
    }

    private def log(resp, xml) {
        // print response
        log.debug "response status: ${resp.statusLine}"
        log.debug 'Headers: -----------'

        resp.headers.each { h -> log.debug " ${h.name} : ${h.value}" }

        log.debug 'Response data: -----'
        log.debug xml
        log.debug '\n--------------------'

        // parse
        assert xml instanceof groovy.util.slurpersupport.GPathResult
    }

    private String filterOutSurroundingTag(String text){
        Pattern pattern = Pattern.compile("<.*>(.+?)</.*>")
        Matcher matcher = pattern.matcher(text)
        matcher.find()
        String out = text
        try{
            out = matcher.group(1)
        }catch(Exception e){}
        return out
    }

    /**
     * Get Data to build Search Result Navigation Bar for Item Detail View
     *
     * @param reqParameters requestParameters
     * @return Map with searchResult to build back + next links
     *  and searchResultUri for Link "Back to Search Result"
     */
    def handleSearchResultParameters(reqParameters, httpRequest) {
        def searchResultParameters = [:]
        searchResultParameters["searchParametersMap"] = [:]
        def resultsItems
        def searchResultUri

        if (reqParameters["hitNumber"] && reqParameters[SearchParamEnum.QUERY.getName()] != null) {
            def urlQuery = searchService.convertQueryParametersToSearchParameters(reqParameters)

            //Search and return 3 Hits: previous, current and last
            reqParameters["hitNumber"] = reqParameters["hitNumber"].toInteger()
            urlQuery[SearchParamEnum.ROWS.getName()] = 3
            if (reqParameters["hitNumber"] > 1) {
                urlQuery[SearchParamEnum.OFFSET.getName()] = reqParameters["hitNumber"] - 2
            }
            else {
                urlQuery[SearchParamEnum.OFFSET.getName()] = 0
            }
            def apiResponse = ApiConsumer.getJson(configurationService.getApisUrl() ,'/apis/search', false, urlQuery)
            if(!apiResponse.isOk()){
                log.error "Json: Json file was not found"
                apiResponse.throwException(request)
            }
            resultsItems = apiResponse.getResponse()

            //Workaround for last-hit (Performance-issue)
            if (reqParameters.id && reqParameters.id.equals(SearchParamEnum.LASTHIT.getName())) {
                reqParameters.id = resultsItems.results["docs"][1].id
            }
            searchResultParameters["resultsItems"] = resultsItems

            //generate link back to search-result. Calculate Offset.
            def searchGetParameters = searchService.getSearchGetParameters(reqParameters)
            def offset = 0
            if (reqParameters[SearchParamEnum.ROWS.getName()]) {
                offset = ((Integer)((reqParameters["hitNumber"]-1)/reqParameters[SearchParamEnum.ROWS.getName()]))*reqParameters[SearchParamEnum.ROWS.getName()]
            }
            searchGetParameters[SearchParamEnum.OFFSET.getName()] = offset
            searchResultUri = grailsLinkGenerator.link(url: [controller: 'search', action: 'results', params: searchGetParameters ])
            searchResultParameters["searchResultUri"] = searchResultUri
            searchResultParameters["searchParametersMap"] = reqParameters
        }

        return searchResultParameters
    }


    private def buildLicenseInformation(def item, httpRequest){
        def licenseInformation

        if(item.item?.license && !item.item.license.isEmpty()){

            def licenseId = getTagAttribute(item.item.license, CortexNamespace.RDF.prefix, "resource")

            def propertyId = convertUriToProperties(licenseId)

            licenseInformation = [:]


            def text
            def url
            def img
            try{
                def locale = SupportedLocales.getBestMatchingLocale(RequestContextUtils.getLocale(httpRequest))
                text = messageSource.getMessage("ddbnext.license.text."+propertyId, null, locale)
                url = messageSource.getMessage("ddbnext.license.url."+propertyId, null, locale)
                img = messageSource.getMessage("ddbnext.license.img."+propertyId, null, locale)
            }catch(NoSuchMessageException e){
                log.error "findById(): no I18N information for license '"+licenseInformation.id+"' in license.properties"
            }
            if(!text){
                text = item.item.license.toString()
            }
            if(!url){
                url = item.item.license["@url"].toString()
            }

            licenseInformation.text = text
            licenseInformation.url = url
            licenseInformation.img = img

        }

        return licenseInformation
    }

    def convertUriToProperties(def uri){
        if(uri){
            // http://creativecommons.org/licenses/by-nc-nd/3.0/de/

            def converted = uri.toString()
            converted = converted.replaceAll("http://","")
            converted = converted.replaceAll("https://","")
            converted = converted.replaceAll("[^A-Za-z0-9]", ".")
            if(converted.startsWith(".")){
                converted = converted.substring(1)
            }
            if(converted.endsWith(".")){
                converted = converted.substring(0, converted.size()-1)
            }
            return converted
        }else{
            return ""
        }
    }

    def String getTagAttribute(def tag, String namespacePrefix, String attributeName ) {
        String out = null
        out = tag["@"+namespacePrefix+":"+attributeName].toString().trim()
        if(out == null || out.isEmpty()){
            out = tag["@"+CortexNamespace.NS2.prefix+":"+attributeName].toString().trim()
        }
        return out
    }

    private boolean isFavorite(itemId) {
        def User user = sessionService.getSessionAttributeIfAvailable(User.SESSION_USER)
        if(user != null) {
            return bookmarksService.isBookmarkOfUser(itemId, user.getId())
        }else{
            return false
        }
    }

    def translate(fields, convertToHtmlLink, httpRequest) {
        def locale = SupportedLocales.getBestMatchingLocale(RequestContextUtils.getLocale(httpRequest))

        fields.each {
            it = convertToHtmlLink(it)
            def messageKey = 'ddbnext.' + it.'@id'

            def translated = messageSource.getMessage(messageKey, null, messageKey, locale)
            if(translated != messageKey) {
                it.name = translated
            } else {
                it.name = it.name.toString().capitalize()
                log.warn 'can not find message property: ' + messageKey + ' use ' + it.name + ' instead.'
            }
        }
    }

    def convertToHtmlLink = { field ->
        for(int i=0; i<field.value.size(); i++) {
            def fieldValue = field.value[i].toString()
            if(fieldValue.startsWith(HTTP) || fieldValue.startsWith(HTTPS)) {
                field.value[i] = '<a href="' + fieldValue + '">' + fieldValue + '</a>'
            }
        }

        return field
    }

    def createEntityLinks(fields){
        fields.each {
            def valueTags = it.value
            valueTags.each { valueTag ->

                def resource = getTagAttribute(valueTag, CortexNamespace.RDF.prefix, "resource")

                if(resource != null && !resource.isEmpty()){
                    if(cultureGraphService.isValidGndUri(resource)){
                        def entityId = cultureGraphService.getGndIdFromGndUri(resource)
                        valueTag.@entityId = entityId
                    }
                }
            }
        }
        return fields
    }

    def delFavorite(itemId) {
        boolean vResult = false
        log.info "non-JavaScript: delFavorite " + itemId
        def User user = sessionService.getSessionAttributeIfAvailable(User.SESSION_USER)
        if (user != null) {
            // Bug: DDBNEXT-626: if (bookmarksService.deleteBookmarksByBookmarkIds(user.getId(), [pId])) {
            bookmarksService.deleteBookmarksByItemIds(user.getId(), [itemId])
            def isFavorite = isFavorite(itemId)
            if (isFavorite == response.SC_NOT_FOUND) {
                log.info "non-JavaScript: delFavorite " + itemId + " - success!"
                vResult = true
            }
            else {
                log.info "non-JavaScript: delFavorite " + itemId + " - failed..."
            }
        }
        else {
            log.info "non-JavaScript: addFavorite " + itemId + " - failed (unauthorized)"
        }
        return vResult
    }

    def addFavorite(itemId) {
        boolean vResult = false
        log.info "non-JavaScript: addFavorite " + itemId
        def User user = sessionService.getSessionAttributeIfAvailable(User.SESSION_USER)
        if (user != null) {
            Bookmark newBookmark = new Bookmark(
                    null,
                    user.getId(),
                    itemId,
                    new Date().getTime(),
                    Type.CULTURAL_ITEM,
                    null,
                    "",
                    new Date().getTime())
            String newBookmarkId = bookmarksService.createBookmark(newBookmark)
            if (newBookmarkId) {
                log.info "non-JavaScript: addFavorite " + itemId + " - success!"
                vResult = true
            }
            else {
                log.info "non-JavaScript: addFavorite " + itemId + " - failed..."
            }
        }
        else {
            log.info "non-JavaScript: addFavorite " + itemId + " - failed (unauthorized)"
        }
        return vResult
    }
}
