package de.ddb.common.constants

public enum SearchParamEnum {


    ROWS("rows"),
    OFFSET("offset"),
    SORT_RELEVANCE("RELEVANCE"),
    SORT_ALPHA_ASC("ALPHA_ASC"),
    SORT_ALPHA_DESC("ALPHA_DESC"),
    SORT_RANDOM("random"),
    SORT("sort"),
    ORDER("order"),
    BY("by"),
    QUERY("query"),
    VIEWTYPE_LIST("list"),
    VIEWTYPE_GRID("grid"),
    VIEWTYPE("viewType"),
    CLUSTERED("clustered"),
    IS_THUMBNAILS_FILTERED("isThumbnailFiltered"),
    FACETVALUES("facetValues[]"),
    FACET("facet"),
    FACETS("facets[]"),
    FIRSTHIT("firstHit"),
    LASTHIT("lastHit"),
    KEEPFILTERS("keepFilters"),
    NORMDATA("normdata"),
    CALLBACK("callback"),
    MINDOCS("minDocs"),



    private String name

    private SearchParamEnum(String name) {
        this.name = name
    }

    public String getName() {
        return name
    }
}
