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

import org.junit.Test


class ItemServiceTests extends GroovyTestCase {

    def itemService

    @Test void findItemByIdTest() {
        def res = itemService.findItemById("UTKNISTTJSEBBXLGXSJV4SY2IU7IQWC4")
        assert res.size() > 0
    }

    /**
     * TODO: Test will fail because a user based seesion is needed
     */
    @Test void getFullItemModelTest() {
        //        def res = itemService.getFullItemModel("UTKNISTTJSEBBXLGXSJV4SY2IU7IQWC4")
        //        println res
        //        assert res.size() > 0
    }

    @Test void findBinariesByIdTest() {
        def res = itemService.findBinariesById("UTKNISTTJSEBBXLGXSJV4SY2IU7IQWC4")
        assert res.size() > 0
    }

    @Test void getParentTest() {
        def res = itemService.getParent("UTKNISTTJSEBBXLGXSJV4SY2IU7IQWC4")
        println res
        assert res.size() > 0
    }

    @Test void getChildrenTest() {
        def res = itemService.getChildren("JXWVQJ36SXFB4K5CO5RFZKJ6LC5VUF65")
        println res
        assert res.size() > 0
    }

    /**
     * TODO: Currently i didn't find an item with xml metadata
     */
    @Test void fetchXMLMetadata() {
        //        def res = itemService.fetchXMLMetadata("UTKNISTTJSEBBXLGXSJV4SY2IU7IQWC4")
        //        println res
        //        assert res.size() > 0
    }
}
