import de.ddb.common.constants.FacetEnum

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
// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'
}

ddb {
    backend {
        facets {
            filter = [
                [facetName:FacetEnum.LANGUAGE.getName(), filter:'term:unknown' ],
                [facetName:FacetEnum.LANGUAGE.getName(), filter:'term:termunknown'],
                [facetName:FacetEnum.KEYWORDS.getName(), filter:'null'],
                [facetName:FacetEnum.PROVIDER.getName(), filter:'null'],
                [facetName:FacetEnum.AFFILIATE.getName(), filter:'null'],
                [facetName:FacetEnum.TYPE.getName(), filter:'null'],
                [facetName:FacetEnum.SECTOR.getName(), filter:'null'],
                [facetName:FacetEnum.PLACE.getName(), filter:'null'],
                [facetName:FacetEnum.TIME.getName(), filter:'null']
            ]
        }
    }
}

environments {
    development {
        grails.logging.jul.usebridge = true
        grails.config.locations = [
            "file:${userHome}/.grails/ddb-next.properties"
        ]
    }
    production {
        grails.logging.jul.usebridge = false
        grails.config.locations = [
            "file:"+ System.getProperty('catalina.base')+ "/grails/app-config/ddb-next.properties"
        ]
    }
    test {
        grails.logging.jul.usebridge = true
        grails.config.locations = [
            "file:${userHome}/.grails/ddb-next.properties"
        ]
    }
    println "| Read properties from " + grails.config.locations[0]
}

//DDB SPECIFIC Configuration variables
//The variables have to be overwritten by defining local configurations, see below environments
ddb.binary.url="http://localhost/binary/"
ddb.static.url="http://localhost/static/"
ddb.apis.url="http://localhost:8080/"
ddb.backend.url="http://localhost/backend:9998/"
ddb.backend.apikey=""
ddb.aas.url="http://localhost/aas:8081/"
ddb.culturegraph.url="http://hub.culturegraph.org"
ddb.bookmark.url="http://localhost:9200"
ddb.newsletter.url="http://localhost:9200"
ddb.elasticsearch.url="http://localhost:9200"
ddb.logging.folder="target/logs"
ddb.tracking.piwikfile="${userHome}/.grails/tracking.txt"
ddb.advancedSearch.searchGroupCount=3
ddb.advancedSearch.searchFieldCount=10
ddb.advancedSearch.defaultOffset=0
ddb.advancedSearch.defaultRows=20
ddb.session.timeout=3600 // in sec -> 60min
ddb.loadbalancer.header.name="nid"
ddb.loadbalancer.header.value="-1"
ddb.favorites.sendmailfrom="noreply@deutsche-digitale-bibliothek.de"
ddb.favorites.reportMailTo=""  // "geschaeftsstelle@deutsche-digitale-bibliothek.de"
ddb.culturegraph.features.enabled=false
ddb.apikey.doc.url="https://api.deutsche-digitale-bibliothek.de/"
ddb.apikey.terms.url="/content/terms/api"
ddb.account.terms.url="/content/terms/ugc"
ddb.account.privacy.url="/content/privacy/personal_data"