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
class DdbCommonGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = []

    def title = "Ddb Common Plugin" // Headline display name of the plugin
    def author = "FIZ Karlsruhe"
    def authorEmail = ""
    def description = '''\
    This plugin provides common classes for DDB projects like DDB-NEXT or APD 
    '''

    // URL to the plugin's documentation
    //def documentation = "http://grails.org/plugin/ddb-common"
}
