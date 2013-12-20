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

import static org.junit.Assert.*

import org.junit.*

import de.ddb.common.beans.Bookmark
import de.ddb.common.beans.Folder
import de.ddb.common.constants.FolderConstants
import de.ddb.common.constants.Type

class BookmarkServiceIntegrationTests extends GroovyTestCase {

    private static final def SIZE = 99999

    def bookmarksService

    /** The userId is refreshed in setUp() for every test method*/
    def userId = null


    /**
     * Is called before every test method.
     * Creates a new userId for the test. 
     */
    void setUp() {
        super.setUp()
        println "####################################################################"
        println "Setup tests"
        userId = UUID.randomUUID() as String
        logStats()
    }

    /**
     * Is called after every test method.
     * Cleanup user content created by a test method
     */
    void tearDown() {
        super.tearDown()
        println "Cleanup tests"

        List<Bookmark> bookmarks = bookmarksService.findBookmarksByUserId(userId)
        println "User bookmarks after test: " + bookmarks.size()

        List<Folder> folders = bookmarksService.findAllFolders(userId)
        println "User folders after test: " + folders.size()

        boolean contentDeleted = bookmarksService.deleteAllUserContent(userId)
        println "User content deleted: : " + contentDeleted

        logStats()
        println "####################################################################"
    }


    def logStats() {
        println "userId " + userId
        println "Index has " + bookmarksService.getFolderCount() + " folders"
        println "Index has " + bookmarksService.getBookmarkCount() + " bookmarks"
    }


    def createNewFolder() {
        def folderTitle= 'Favorites-' + new Date().getTime().toString()
        def isPublic = true
        def publishingName = FolderConstants.PUBLISHING_NAME_USERNAME.getValue()
        Folder newFolder = new Folder(
                null,
                userId,
                folderTitle,
                "",
                isPublic,
                publishingName,
                false,
                "")
        return bookmarksService.createFolder(newFolder)
    }


    // Folder
    @Test void shouldCreateNewFolder() {
        def folderId = createNewFolder()
        assertNotNull folderId
        log.info "Created a bookmark folder with the ID: ${folderId}"
    }

    @Test void shouldGetAllFolders() {
        def folderId = createNewFolder()

        def folderList = bookmarksService.findAllFolders(userId)
        assertTrue folderList.size() >0

        if(folderList) {
            log.info "The user with the ID: ${userId} has: "
            folderList.each { it ->
                log.info "- ${it}"
            }
        } else {
            log.info 'empty folder.'
        }
    }

    // Bookmark
    @Test void shouldSaveBookmarkInFolder() {
        def folderId = createNewFolder()
        def itemId = 'foobar'

        Bookmark newBookmark = new Bookmark(null, userId, itemId, new Date().getTime(), Type.CULTURAL_ITEM, [folderId], "", new Date().getTime())
        def bookmarkId = bookmarksService.createBookmark(newBookmark)

        assertNotNull bookmarkId
        log.info 'bookmark is saved, ID is: ' + bookmarkId
    }

    @Test void shouldFindBookmarksByFolderId() {
        def folderId = createNewFolder()
        def itemId = 'foobarbaz'
        Bookmark newBookmark = new Bookmark(null, userId, itemId, new Date().getTime(), Type.CULTURAL_ITEM, [folderId], "", new Date().getTime())
        def bookmarkId = bookmarksService.createBookmark(newBookmark)
        def bookmarks = bookmarksService.findBookmarksByFolderId(userId, folderId)
        assert bookmarks.size() > 0
    }

    @Test void shouldFindBookmarkedItems() {
        def folderId = createNewFolder()
        def itemId = 'F2D23TGU7NMP5MGVF647Q63X3E32W4YI'
        Bookmark newBookmark = new Bookmark(null, userId, itemId, new Date().getTime(), Type.CULTURAL_ITEM, [folderId], "", new Date().getTime())
        def bookmarkId = bookmarksService.createBookmark(newBookmark)

        def foundBookmarkedItems = bookmarksService.findBookmarksForItemIds(userId, [itemId])
        assert foundBookmarkedItems.size() > 0
    }

    // Favorites
    @Test void shouldAddItemToUserFavorite() {
        log.info "should add item to the user's Favorites"

        def itemId = UUID.randomUUID() as String
        def folderId = 'foo'

        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                itemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        String favoriteId = bookmarksService.createBookmark(newBookmark)
        assert favoriteId != null

        log.info "The user ${userId} just added item ${itemId} to their Favorites folder favoriteId"
    }

    @Test void shouldFindFoldersByTitle() {
        log.info "the bookmark service should find folders by its title."

        Folder newFolder = new Folder(
                null,
                userId,
                FolderConstants.MAIN_BOOKMARKS_FOLDER.getValue(),
                "",
                BookmarksService.IS_PUBLIC,
                FolderConstants.PUBLISHING_NAME_USERNAME.getValue(),
                false,
                "")
        String folderId = bookmarksService.createFolder(newFolder)
        log.info "the bookmark service created a ${FolderConstants.MAIN_BOOKMARKS_FOLDER.value} folder(${folderId}) for a user(${userId})"

        def favFolderList = bookmarksService.findFoldersByTitle(userId, FolderConstants.MAIN_BOOKMARKS_FOLDER.value)
        log.info "The user(${userId}) has ${favFolderList.size()} folders with the title `Favorites`"

        if(!favFolderList) {
            favFolderList = bookmarksService.findFoldersByTitle(userId, FolderConstants.MAIN_BOOKMARKS_FOLDER.value)
            log.info "Second try, the user(${userId}) has ${favFolderList.size()} folders with the title `Favorites`"
        }

        assert favFolderList.size() == 1
        assertEquals favFolderList[0].folderId, folderId
    }

    @Test void shouldGetAllUserFavorites() {
        def firstItemId = 'foobarbaz'
        def secondItemId = UUID.randomUUID() as String
        def folderId = 'foo'

        // if the user don't have a favorite list, then the service should create it.
        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                firstItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def firstFav = bookmarksService.createBookmark(newBookmark)

        def allFavs = bookmarksService.findBookmarksByUserId(userId)
        assert allFavs.size() > 0
    }

    @Test void shouldDeleteFavoritesByItemIDs() {
        log.info "the bookmark service should delete favorites by item IDs."

        def firstItemId = 'foobarbaz'
        def folderId = 'foo'

        log.info "adding item ${firstItemId} to the folder Favorite."

        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                firstItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def firstFav = bookmarksService.createBookmark(newBookmark)
        def allFavs = bookmarksService.findBookmarksByUserId(userId)
        assert allFavs.size() == 1
        assert allFavs[0].itemId == firstItemId

        def itemIds = [firstItemId]
        assert bookmarksService.deleteBookmarksByItemIds(userId, itemIds) == true

        def emptyFavs = bookmarksService.findBookmarksByUserId(userId)
        assert emptyFavs.size() == 0
    }

    @Test void shouldFindFavoritesByItemIds() {
        log.info "the bookmark service should find favorites by item IDs."

        def folderId = 'foo'

        def firstItemId = 'F2D23TGU7NMP5MGVF647Q63X3E32W4YIn84O2mBlSiassU1aNYIysA'
        log.info "adding item ${firstItemId} to the folder Favorite."
        Bookmark firstBookmark = new Bookmark(
                null,
                userId,
                firstItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def firstFavId = bookmarksService.createBookmark(firstBookmark)

        def secondItemId = 'U3TWCZVFIHOC6A65ICIC3UIEYRCR2LKL'
        log.info "adding item ${secondItemId} to the folder Favorite."
        Bookmark secondBookmark = new Bookmark(
                null,
                userId,
                secondItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def secondFavId = bookmarksService.createBookmark(secondBookmark)

        def itemIds = [firstItemId, secondItemId]

        def foundBookmarkedItems = bookmarksService.findBookmarkedItemsInFolder(userId, itemIds, null)
        assert foundBookmarkedItems.size() > 0
    }

    @Test void shouldNotAddSameItemIdMoreThanOnce() {
        log.info "the bookmark service should _not_ add the same item IDs more than once to favorites."

        def folderId = 'foo'

        def firstItemId = 'F2D23TGU7NMP5MGVF647Q63X3E32W4YI'
        log.info "adding item ${firstItemId} to the folder Favorite."
        Bookmark firstBookmark = new Bookmark(
                null,
                userId,
                firstItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def firstFavId = bookmarksService.createBookmark(firstBookmark)

        def secondItemId = 'F2D23TGU7NMP5MGVF647Q63X3E32W4YI'
        log.info "adding item ${secondItemId} _again_ to the folder Favorite."
        Bookmark secondBookmark = new Bookmark(
                null,
                userId,
                secondItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def secondFavId = bookmarksService.createBookmark(secondBookmark)

        assert secondFavId == null
    }

    @Test void shouldFindFavoriteByItemId() {
        log.info "the bookmark service should find favorite by item ID"

        def folderId = 'foo'

        def firstItemId = 'F2D23TGU7NMP5MGVF647Q63X3E32W4YI'
        log.info "adding item ${firstItemId} to the folder Favorite."
        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                firstItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def firstFavId = bookmarksService.createBookmark(newBookmark)

        def favoritesForItem = bookmarksService.findBookmarkedItemsInFolder(userId, [firstItemId], null)
        assert favoritesForItem != null
        assert favoritesForItem.size() == 1
        assert favoritesForItem[0].itemId == firstItemId
        log.info favoritesForItem
    }

    @Test void shouldReturnMoreThanTenFavorites() {
        log.info "the bookmark service should find more than 10 favorites"

        def folderId = 'foo'

        11.times {
            def itemId = UUID.randomUUID() as String
            Bookmark newBookmark = new Bookmark(
                    null,
                    userId,
                    itemId,
                    new Date().getTime(),
                    Type.CULTURAL_ITEM,
                    [folderId],
                    "",
                    new Date().getTime())
            def favId = bookmarksService.createBookmark(newBookmark)
            log.info("Bookmark ${favId} is created." )
        }

        def allFavs = bookmarksService.findBookmarksByUserId(userId)
        log.error('all favorites is more than 10: ' + allFavs.size())
        assert allFavs.size() > 10
    }

    @Test void shouldSaveInstitutionAsFavorites() {
        log.info "should save institution as user's Favorites"
        // should add a cultural item to user's favorite list.

        def institutionId = UUID.randomUUID() as String
        def folderId = 'foo'

        // if the user don't have a favorite list, then the service should create it.
        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                institutionId,
                new Date().getTime(),
                Type.INSTITUTION,
                [folderId],
                "",
                new Date().getTime())
        def favoriteId = bookmarksService.createBookmark(newBookmark)
        assert favoriteId != null
        log.info "The user ${userId} just added an institution ${institutionId} to their Favorites folder(favoriteId)"

        def favoritesForInstitution = bookmarksService.findBookmarkedItemsInFolder(userId, [institutionId], null)
        log.info("fav is: ${favoritesForInstitution }")
        assert favoritesForInstitution.size() == 1
        assert favoritesForInstitution[0].itemId == institutionId
        assert favoritesForInstitution[0].type == Type.INSTITUTION
    }

    // TODO: problem Favorites VS Bookmarks?
    // @Test void shouldSaveFavoritesWithOptionalTitleOrDescription() { assert false }
    // @Test void shouldFindFavoritesByUserIdAndFolderId() { assert false }

    @Test void shouldCreateNewFolderWithDescription() {

        def folderTitle = 'Favorites-' + new Date().getTime().toString()
        def description = 'folder description'
        def publishingName = FolderConstants.PUBLISHING_NAME_USERNAME.value
        def isPublic = true

        Folder newFolder = new Folder(
                null,
                userId,
                folderTitle,
                description,
                isPublic,
                publishingName,
                false,
                "")
        String folderId = bookmarksService.createFolder(newFolder)

        def folders = bookmarksService.findAllFolders(userId)
        assert folders[0].description == description

        log.info "folder's description ${folders[0].description}"
    }


    @Test void shouldFindFavoriteById() {

        def itemId = UUID.randomUUID() as String

        def folderTitle= 'Favorites-' + new Date().getTime().toString()
        def isPublic = true

        Folder newFolder = new Folder(
                null,
                userId,
                folderTitle,
                "",
                isPublic,
                FolderConstants.PUBLISHING_NAME_USERNAME.getValue(),
                false,
                "")
        String folderId = bookmarksService.createFolder(newFolder)
        Bookmark newBookmark = new Bookmark(
                null,
                userId,
                itemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def favoriteId = bookmarksService.createBookmark(newBookmark)

        assert favoriteId != null
        log.info "The user ${userId} just added item ${itemId} to their Favorites folder favoriteId ${favoriteId}"

        def favorite = bookmarksService.findBookmarkById(favoriteId)
        assert favorite.bookmarkId == favoriteId
    }

    //    @Test void shouldCopyFavoritesToFolders() {
    //        log.info "should copy more than one favorites to more than one folder."
    //        def folderTitle = 'foo'
    //
    //        // create a favorite that not belongs to any folder, i.e. folder: [].
    //        userId = UUID.randomUUID() as String
    //        def itemId = UUID.randomUUID() as String
    //        def favoriteId = bookmarksService.addBookmark(userId, itemId, null, Type.CULTURAL_ITEM, [folderTitle])
    //
    //        // create two folders
    //        def folderId = createNewFolder()
    //        def otherFolderId = createNewFolder()
    //
    //        // copy the favorite to the new folder, i.e., folder: [${folderId}]
    //        bookmarksService.copyBookmarksToFolders([favoriteId], [folderId, otherFolderId])
    //        def found = bookmarksService.findBookmarkById(favoriteId)
    //        log.info "found: ${found.bookmarkId}"
    //        assert found.folders.size() == 3
    //    }

    @Test void shouldChangeFolderTitleOrDescription() {
        log.info "should change folder's title or its description."

        def folderTitle = 'foo'

        Folder newFolder = new Folder(
                null,
                userId,
                folderTitle,
                "",
                BookmarksService.IS_PUBLIC,
                FolderConstants.PUBLISHING_NAME_USERNAME.getValue(),
                false,
                "")
        String folderId = bookmarksService.createFolder(newFolder)
        log.info "the bookmark service created a ${folderTitle} folder(${folderId}) for a user(${userId})"

        def newTitle = "bar"
        def newDescription = "new desc"

        newFolder.folderId = folderId
        newFolder.title = newTitle
        newFolder.description = newDescription
        bookmarksService.updateFolder(newFolder)

        def updatedFolder = bookmarksService.findFolderById(folderId)
        assert updatedFolder.title == newTitle
        assert updatedFolder.description == newDescription
    }

    @Test void shouldRemoveFavoritesFromFolder() {
        log.info "should remove a few favorites from a folder."

        def folderTitle = 'foo'

        Folder newFolder = new Folder(
                null,
                userId,
                folderTitle,
                "",
                BookmarksService.IS_PUBLIC,
                FolderConstants.PUBLISHING_NAME_USERNAME.getValue(),
                false,
                "")
        String folderId = bookmarksService.createFolder(newFolder)
        log.info "the bookmark service created a ${folderTitle} folder(${folderId}) for a user(${userId})"

        def itemId = UUID.randomUUID() as String
        def otherItemId = UUID.randomUUID() as String

        // create two favorites
        Bookmark firstBookmark = new Bookmark(
                null,
                userId,
                itemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def favoriteId = bookmarksService.createBookmark(firstBookmark)
        Bookmark secondBookmark = new Bookmark(
                null,
                userId,
                otherItemId,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def otherFavoriteId = bookmarksService.createBookmark(secondBookmark)

        def favorites = bookmarksService.findBookmarksByUserId(userId)

        favorites.each { it ->
            assert it.folders[0] == folderId
        }

        bookmarksService.removeBookmarksFromFolder([favoriteId, otherFavoriteId], folderId)

        def favorite = bookmarksService.findBookmarkById(favoriteId)
        assert favorite == null

        def otherFavorite = bookmarksService.findBookmarkById(otherFavoriteId)
        assert otherFavorite == null
    }

    @Test void shouldDeleteFolder() {
        def folderId = createNewFolder()

        def aNewFolder = bookmarksService.findFolderById(folderId)
        assert aNewFolder.folderId == folderId

        bookmarksService.deleteFolder(folderId)

        def folder = bookmarksService.findFolderById(folderId)
        assert folder == null
    }

    @Test void shouldDeleteAllUserFavorites() {
        def folderId = 'foo'

        Bookmark firstBookmark = new Bookmark(
                null,
                userId,
                UUID.randomUUID() as String,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def favoriteId = bookmarksService.createBookmark(firstBookmark)
        Bookmark secondBookmark = new Bookmark(
                null,
                userId,
                UUID.randomUUID() as String,
                new Date().getTime(),
                Type.CULTURAL_ITEM,
                [folderId],
                "",
                new Date().getTime())
        def otherFavId = bookmarksService.createBookmark(secondBookmark)

        bookmarksService.deleteAllUserBookmarks(userId)

        def userFavs = bookmarksService.findBookmarksByUserId(userId)

        assert userFavs.size() == 0
    }

    @Test void shouldDeleteAllUserFolders() {
        createNewFolder()
        createNewFolder()

        def userFolder = bookmarksService.findAllFolders(userId)
        assert userFolder.size() == 2

        bookmarksService.deleteAllUserFolders(userId)

        userFolder = bookmarksService.findAllFolders(userId)
        assert userFolder.size() == 0
    }
}
