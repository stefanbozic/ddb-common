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
package de.ddb.common.constants


/**
 * Enum for the facets.
 * 
 * @author boz
 */
public enum FacetEnum {
    //Main facets
    TIME("time_fct", "ddbnext.time_fct_", true),
    PLACE("place_fct", null, true),
    AFFILIATE("affiliate_fct", null, true),
    KEYWORDS("keywords_fct", null, true),
    LANGUAGE("language_fct", "ddbnext.language_fct_", true),
    TYPE("type_fct", "ddbnext.type_fct_", true),
    SECTOR("sector_fct", "ddbnext.sector_fct_", true),
    PROVIDER("provider_fct", null, true),
    AFFILIATE_INVOLVED("affiliate_fct_involved", null, false),
    AFFILIATE_SUBJECT("affiliate_fct_subject", null, false),
    AFFILIATE_INVOLVED_NORMDATA("affiliate_fct_involved_normdata", null, false),
    AFFILIATE_SUBJECT_NORMDATA("affiliate_fct_subject_normdata", null, false)

    /** The facet name as used by the cortex */
    private String name

    /** The i18n prefix for this facet  */
    private String i18nPrefix = null

    /** Indicates if this facet is used in the item search */
    private boolean isSearchFacet

    /**
     * Constructor
     * 
     * @param name name of the facet
     * @param isSearchFacet <code>true</code> if this facet is used in the item search
     */
    private FacetEnum(String name, String i18nPrefix, boolean isSearchFacet) {
        this.name = name
        this.i18nPrefix = i18nPrefix
        this.isSearchFacet = isSearchFacet
    }

    /**
     * Return the name of the enum
     *  
     * @return the name of the enum
     */
    public String getName() {
        return name
    }

    /**
     * Indicates if this facet is used for item search 
     * 
     * @return <code>true</code> if this enum is used for item search
     */
    public boolean isSearchFacet() {
        return isSearchFacet
    }

    /**
     * Gets the i18nPrefix for the values in the resource bundles
     * @return the i18nPrefix for the values in the resource bundles
     */
    public String getI18nPrefix() {
        return i18nPrefix
    }

}
