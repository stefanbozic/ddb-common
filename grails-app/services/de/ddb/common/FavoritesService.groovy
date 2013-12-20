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

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.*
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.context.request.RequestContextHolder

import de.ddb.common.beans.Folder
import de.ddb.common.beans.User
import de.ddb.common.constants.SearchParamEnum

class FavoritesService {

    def transactional = false
    def bookmarksService
    def sessionService
    def grailsApplication
    def searchService
    def configurationService
    def messageSource

    def createAllFavoritesLink(Integer offset, Integer rows, String order, String by, Integer lastPgOffset, String folderId){
        def first = createFavoritesLinkNavigation(0, rows, order, by, folderId)
        if (offset < rows){
            first = null
        }
        def last = createFavoritesLinkNavigation(lastPgOffset, rows, order, by, folderId)
        if (offset >= lastPgOffset){
            last = null
        }
        return [
            firstPg: first,
            prevPg: createFavoritesLinkNavigation(offset.toInteger()-rows, rows, order, by, folderId),
            nextPg: createFavoritesLinkNavigation(offset.toInteger()+rows, rows, order, by, folderId),
            lastPg: last
        ]
    }
    def private createFavoritesLinkNavigation(offset,rows,order,by,folderId){
        def g = grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
        return g.createLink(controller:'favorites', action: 'favorites',params:[(SearchParamEnum.OFFSET.getName()):offset,(SearchParamEnum.ROWS.getName()):rows, (SearchParamEnum.ORDER.getName()):order, (SearchParamEnum.BY.getName()):by,id:folderId])
    }

    def createAllPublicFavoritesLink(Integer offset, Integer rows, String order, String by, Integer lastPgOffset, String userId, String folderId){
        def first = createPublicFavoritesLinkNavigation(0, rows, order, userId, folderId, by)
        if (offset < rows){
            first = null
        }
        def last = createPublicFavoritesLinkNavigation(lastPgOffset, rows, order, userId, folderId, by)
        if (offset >= lastPgOffset){
            last = null
        }
        return [
            firstPg: first,
            prevPg: createPublicFavoritesLinkNavigation(offset.toInteger()-rows, rows, order, userId, folderId, by),
            nextPg: createPublicFavoritesLinkNavigation(offset.toInteger()+rows, rows, order, userId, folderId, by),
            lastPg: last
        ]
    }
    def private createPublicFavoritesLinkNavigation(Integer offset, Integer rows, String order, String userId, String folderId, String by){
        def g = grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
        return g.createLink(controller:'favorites', action: 'publicFavorites', params:[userId: userId, folderId: folderId, (SearchParamEnum.OFFSET.getName()):offset, (SearchParamEnum.ROWS.getName()):rows, (SearchParamEnum.ORDER.getName()):order, (SearchParamEnum.BY.getName()):by])
    }

    /**
     * Retrieve from Backend the Metadata for the items retrieved from the favorites list
     * @param items
     * @return
     */
    def retriveItemMD(List items, Locale locale){
        def step = 20
        def orQuery=""
        def allRes = []

        items.eachWithIndex() { it, i ->
            if ( (i==0) || ( ((i>1)&&(i-1)%step==0)) ){
                orQuery=it.itemId
            }else if (i%step==0){
                orQuery=orQuery + " OR "+ it.itemId
                queryBackend(orQuery, locale).each { item ->
                    allRes.add(item)
                }
                orQuery=""
            }else{
                orQuery+=" OR "+ it.itemId
            }
        }
        if (orQuery){
            queryBackend(orQuery,locale).each { item ->
                allRes.add(item)
            }
        }

        // Add empty items for all orphaned elasticsearch bookmarks
        if(items.size() > allRes.size()){
            def g = grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
            def dummyThumbnail = g.resource("dir": "images", "file": "/placeholder/searchResultMediaUnknown.png").toString()
            def label = messageSource.getMessage("ddbnext.Item_No_Longer_Exists", null, LocaleContextHolder.getLocale())

            def foundItemIds = allRes.collect{ it.id }
            items.each{
                // item not found
                if(!(it.itemId in foundItemIds)){

                    def emptyDummyItem = [:]
                    emptyDummyItem["id"] = it.itemId
                    emptyDummyItem["view"] = []
                    emptyDummyItem["label"] = label
                    emptyDummyItem["latitude"] = ""
                    emptyDummyItem["longitude"] = ""
                    emptyDummyItem["category"] = "orphaned"
                    emptyDummyItem["preview"] = [:]
                    emptyDummyItem["preview"]["title"] = label
                    emptyDummyItem["preview"]["subtitle"] = ""
                    emptyDummyItem["preview"]["media"] = ["unknown"]
                    emptyDummyItem["preview"]["thumbnail"] = dummyThumbnail

                    net.sf.json.JSONObject jsonDummyItem = (net.sf.json.JSONObject) emptyDummyItem
                    allRes.add(jsonDummyItem)
                }
            }
        }

        return allRes
    }

    def private queryBackend(String query, Locale locale){
        def params = RequestContextHolder.currentRequestAttributes().params
        params.query = "id:("+query+")"

        def urlQuery = searchService.convertQueryParametersToSearchParameters(params)
        urlQuery[SearchParamEnum.OFFSET.getName()]=0
        urlQuery[SearchParamEnum.ROWS.getName()]=21
        def apiResponse = ApiConsumer.getJson(configurationService.getApisUrl() ,'/apis/search', false, urlQuery)
        if(!apiResponse.isOk()){
            log.error "Json: Json file was not found"
            apiResponse.throwException(request)
        }
        def resultsItems = apiResponse.getResponse()

        //Replacing the mediatype images when not coming from backend server
        resultsItems = searchService.checkAndReplaceMediaTypeImages(resultsItems)

        return resultsItems["results"]["docs"]
    }

    def getAllFoldersPerUser(User user){
        if (user != null) {
            return bookmarksService.findAllFolders(user.getId())
        }
        else {
            log.info "getFavorites returns " + response.SC_UNAUTHORIZED
            return null
        }
    }

    List addBookmarkToFavResults(allRes, List items, Locale locale) {
        def all = []
        def temp = []
        allRes.each { searchItem->
            temp = []
            temp = searchItem
            for(int i=0; i<items.size(); i++){
                if(items.get(i).itemId == searchItem.id){
                    temp["bookmark"] = items.get(i).getAsMap()
                    temp["bookmark"]["creationDateFormatted"] = formatDate(items.get(i).creationDate, locale)
                    temp["bookmark"]["updateDateFormatted"] = formatDate(items.get(i).updateDate, locale)
                    break
                }
            }
            all.add(temp)
        }
        return all
    }

    List addFolderToFavResults(allRes, Folder folder) {
        def all = []
        def temp = []
        allRes.each { searchItem->
            temp = searchItem
            temp["folder"] = folder.getAsMap()
            all.add(temp)
        }
        return all
    }

    private String formatDate(Date oldDate, Locale locale) {
        SimpleDateFormat newFormat = new SimpleDateFormat("dd.MM.yyy HH:mm")
        newFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"))
        return newFormat.format(oldDate)
    }

    List addCurrentUserToFavResults(allRes, User user) {
        def userJson = [:]
        userJson["id"] = user.id
        userJson["username"] = user.username
        userJson["status"] = user.status
        userJson["firstname"] = user.firstname
        userJson["lastname"] = user.lastname
        userJson["email"] = user.email

        allRes.each { searchItem ->
            searchItem["user"] = userJson
        }
        return allRes
    }

}
