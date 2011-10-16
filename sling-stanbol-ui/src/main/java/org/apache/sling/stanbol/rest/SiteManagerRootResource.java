/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.stanbol.rest;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Resource to provide a REST API for the {@link ReferencedSiteManager}.
 * 
 * TODO: add description
 */
@Component(immediate = true, metatype = false)
@Service(Object.class)
@Properties({ @Property(name = "javax.ws.rs", boolValue = true) })
@Path("stanbol/entityhub/sites")
public class SiteManagerRootResource {
	
	@Reference
	private ReferencedSiteManager referencedSiteManager;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(N3, N_TRIPLE,
        RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    /**
     * The Field used for find requests if not specified TODO: Will be depreciated as soon as EntityQuery is
     * implemented
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();

    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;


/*    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getSitesPage() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }*/

// removed to allow request with Accept headers other than text/html to return
// the JSON array
//    @GET
//    @Path("/referenced")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getReferencedSitesPage() {
//        return Response.ok(new Viewable("referenced", this), TEXT_HTML).build();
//    }
    
    /**
     * Getter for the id's of all referenced sites
     * 
     * @return the id's of all referenced sites.
     */
    @GET
    @Path(value = "/referenced")
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getReferencedSites(@Context UriInfo uriInfo,
                                        @Context HttpHeaders headers) {
        MediaType acceptable = JerseyUtils.getAcceptableMediaType(headers,
           Arrays.asList(MediaType.APPLICATION_JSON,MediaType.TEXT_HTML) ,
           MediaType.APPLICATION_JSON_TYPE);
        /*if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptable)){
        	return Response.ok(new Viewable("referenced", this), TEXT_HTML).build();
        } else {*/
            JSONArray referencedSites = new JSONArray();
            for (String site : referencedSiteManager.getReferencedSiteIds()) {
                referencedSites.put(String.format("%sentityhub/site/%s/", uriInfo.getBaseUri(), site));
            }
            return Response.ok(referencedSites,acceptable).build();
        //}
    }
    
    /**
     * Cool URI handler for Signs.
     * 
     * @param id
     *            The id of the entity (required)
     * @param headers
     *            the request headers used to get the requested {@link MediaType}
     * @return a redirection to either a browser view, the RDF meta data or the raw binary content
     */
    @GET
    @Path("/entity")
    public Response getEntityById(@QueryParam(value = "id") String id, @Context HttpHeaders headers) {
        log.debug("getSignById() request\n\t> id       : {}\n\t> accept   : {}\n\t> mediaType: {}",
            new Object[] {id, headers.getAcceptableMediaTypes(), headers.getMediaType()});

        Collection<String> supported = new HashSet<String>(JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if (id == null || id.isEmpty()) {
            /*if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return Response.ok(new Viewable("entity", this), TEXT_HTML).build();        
            } else {*/
                return Response.status(Status.BAD_REQUEST)
                    .entity("No or empty ID was parsed. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            //}
        }
        Entity sign;
        // try {
        sign = referencedSiteManager.getEntity(id);
        // } catch (IOException e) {
        // log.error("IOException while accessing ReferencedSiteManager",e);
        // throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        // }
        if (sign != null) {
            return Response.ok(sign, acceptedMediaType).build();
        } else {
            // TODO: How to parse an ErrorMessage?
            // create an Response with the the Error?
            log.info("getSignById() entity {} not found on any referenced site");
            return Response.status(Status.NOT_FOUND)
                .entity("Entity with ID '"+id+"' not found an any referenced site\n")
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
    }

//    @GET
//    @Path("/find")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getFindPage() {
//        return Response.ok(new Viewable("find", this), TEXT_HTML).build();
//    }
    
    @GET
    @Path("/find")
    public Response findEntityfromGet(@QueryParam(value = "name") String name,
                                      @QueryParam(value = "field") String field,
                                      @QueryParam(value = "lang") String language,
                                      // @QueryParam(value="select") String select,
                                      @QueryParam(value = "limit") @DefaultValue(value = "-1") int limit,
                                      @QueryParam(value = "offset") @DefaultValue(value = "0") int offset,
                                      @Context HttpHeaders headers) {
        return findEntity(name, field, language, limit, offset, headers);
    }

    @POST
    @Path("/find")
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String field,
                               @FormParam(value = "lang") String language,
                               // @FormParam(value="select") String select,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               @Context HttpHeaders headers) {
        log.debug("findEntity() Request");
        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            /*if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return Response.ok(new Viewable("find", this), TEXT_HTML).build();        
            } else {*/
                return Response.status(Status.BAD_REQUEST)
                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            //}
        }
        if (field == null) {
            field = DEFAULT_FIND_FIELD;
        } else {
            field = field.trim();
            if (field.isEmpty()) {
                field = DEFAULT_FIND_FIELD;
            }
        }
        FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(name, field, language,
            limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset);
        return Response.ok(referencedSiteManager.find(query), acceptedMediaType).build();
    }
    /*@GET
    @Path("/query")
    public Response getQueryDocumentation(){
        return Response.ok(new Viewable("query", this), TEXT_HTML).build();        
    }*/
    /**
     * Allows to parse any kind of {@link FieldQuery} in its JSON Representation.
     * <p>
     * TODO: as soon as the entityhub supports multiple query types this need to be refactored. The idea is
     * that this dynamically detects query types and than redirects them to the referenced site
     * implementation.
     * 
     * @param query
     *            The field query in JSON format
     * @param headers
     *            the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    @Consumes( {MediaType.APPLICATION_JSON})
    public Response queryEntities(FieldQuery query,
                                  @Context HttpHeaders headers) {

        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(query == null){
            //if query is null nd the mediaType is HTML we need to print the
            //Documentation of the RESTful API
            /*if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return getQueryDocumentation();        
            } else {*/
                return Response.status(Status.BAD_REQUEST)
                    .entity("The query must not be null nor empty for query requests. Missing parameter query.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            //}
        } else {
            return Response.ok(referencedSiteManager.find(query), acceptedMediaType).build();
        }
    }

}