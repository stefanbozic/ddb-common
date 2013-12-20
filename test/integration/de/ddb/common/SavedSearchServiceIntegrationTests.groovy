package de.ddb.common

import static org.junit.Assert.*

import org.junit.*

class SavedSearchServiceIntegrationTests extends GroovyTestCase {

    def savedSearchService

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

        def results = savedSearchService.findSavedSearchByUserId(userId)
        println "Saved user searches after test: " + results.size()

        savedSearchService.deleteSavedSearchesByUserId(userId)

        logStats()
        println "####################################################################"
    }


    def logStats() {
        println "userId " + userId
        println "Index has " + savedSearchService.getSavedSearchesCount() + " searches"
    }

    @Test
    void shouldSavedUserSearch() {
        log.info "should saved user search"

        def queryString = 'query=goethe&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'

        def savedSearchId = savedSearchService.saveSearch(userId, queryString)
        log.info "id: ${savedSearchId}"
        assert savedSearchId  != null
    }

    @Test
    void shouldSavedUserSearchWithTitleAndDescription() {
        log.info "should saved user search"

        def queryString = 'query=goethe&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'

        def savedSearchId = savedSearchService.saveSearch(userId, queryString, 'Goethe Related', 'All things related to Goethe')
        log.info "id: ${savedSearchId}"
        assert savedSearchId  != null
    }

    @Test
    void shouldFindAllSavedSearchesByUserId() {
        log.info "should find all saved searches by user ID"

        def queryStringForGoethe = 'query=goethe&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def goetheSavedSearchId = savedSearchService.saveSearch(userId, queryStringForGoethe , 'Goethe Related', 'All things related to Goethe')
        assert goetheSavedSearchId != null

        def queryStringForMozart = 'query=mozart&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def mozartSavedSearchId = savedSearchService.saveSearch(userId, queryStringForMozart , 'Mozart Related')
        assert mozartSavedSearchId != null

        def results = savedSearchService.findSavedSearchByUserId(userId)
        assert results.size() == 2
    }

    @Test
    void shouldDeleteSavedSearches() {
        log.info "should delete saved search by IDs"

        def queryStringForGoethe = 'query=goethe&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def goetheSavedSearchId = savedSearchService.saveSearch(userId, queryStringForGoethe , 'Goethe Related', 'All things related to Goethe')
        assert goetheSavedSearchId != null

        def queryStringForMozart = 'query=mozart&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def mozartSavedSearchId = savedSearchService.saveSearch(userId, queryStringForMozart , 'Mozart Related')
        assert mozartSavedSearchId != null


        def results = savedSearchService.findSavedSearchByUserId(userId)
        assert results.size() == 2

        savedSearchService.deleteSavedSearch(
                [
                    goetheSavedSearchId,
                    mozartSavedSearchId
                ])

        assert savedSearchService.findSavedSearchByUserId(userId).size() == 0
    }

    @Test
    void shouldDeleteSavedSearchesByUserId() {
        def queryStringForGoethe = 'query=goethe&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def goetheSavedSearchId = savedSearchService.saveSearch(userId, queryStringForGoethe , 'Goethe Related', 'All things related to Goethe')
        assert goetheSavedSearchId != null

        def queryStringForMozart = 'query=mozart&sort=ALPHA_ASC&facetValues[]=time_fct%3Dtime_62000&facetValues[]=time_fct%3Dtime_61600&facetValues[]=keywords_fct%3DFotos&facetValues[]=type_fct%3Dmediatype_002&facetValues[]=sector_fct%3Dsec_02'
        def mozartSavedSearchId = savedSearchService.saveSearch(userId, queryStringForMozart , 'Mozart Related')
        assert mozartSavedSearchId != null


        def results = savedSearchService.findSavedSearchByUserId(userId)
        assert results.size() == 2

        savedSearchService.deleteSavedSearchesByUserId(userId)

        assert savedSearchService.findSavedSearchByUserId(userId).size() == 0
    }

    // TODO: shouldUpdateSavedSearch
}
