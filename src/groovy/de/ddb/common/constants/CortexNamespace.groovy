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

public enum CortexNamespace {

    RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    NS2("ns2", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"), // TODO remove as soon as the cortex delivers fixed namespace prefixes
    OAI_ID("oai_id", "http://www.openarchives.org/OAI/2.0/oai-identifier"),
    OAI_DC("oai_dc", "http://www.deutsche-digitale-bibliothek.de/institution"),
    DC("dc", "http://purl.org/dc/elements/1.1/"),
    CORTEX("cortex", "http://www.deutsche-digitale-bibliothek.de/cortex"),
    CI("ci", "http://www.deutsche-digitale-bibliothek.de/item"),
    XSI("xsi", "http://www.w3.org/2001/XMLSchema-instance"),
    DCTERMS("dcterms", "http://purl.org/dc/terms/"),
    SKOS("skos", "http://www.w3.org/2004/02/skos/core#"),
    EDM("edm", "http://www.europeana.eu/schemas/edm/"),
    DDB("ddb", "http://www.deutsche-digitale-bibliothek.de/edm/"),
    C_ALT("c_alt", "http://www.ddb.de/"),
    CRM("crm", "http://www.cidoc-crm.org/cidoc-crm/"),
    CRM_LABEL("crm_label", "http://www.cidoc-crm.org/rdfs/cidoc_crm_v5.0.2_english_label.rdfs#"),
    ORE("ore", "http://www.openarchives.org/ore/terms/"),
    FOAF("foaf", "http://xmlns.com/foaf/0.1/")



    String prefix
    String uri

    private CortexNamespace(prefix, uri) {
        this.prefix = prefix
        this.uri = uri
    }
}
