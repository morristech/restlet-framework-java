/**
 * Copyright 2005-2014 Restlet
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL
 * 1.0 (the "Licenses"). You can select the license that you prefer but you may
 * not use this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.restlet.com/products/restlet-framework
 * 
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.swagger;

import org.restlet.*;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.cors.CorsResponseHelper;
import org.restlet.ext.apispark.internal.introspection.ApplicationIntrospector;
import org.restlet.ext.apispark.internal.conversion.SwaggerTranslator;
import org.restlet.ext.apispark.internal.model.Definition;
import org.restlet.ext.apispark.internal.model.swagger.ApiDeclaration;
import org.restlet.ext.apispark.internal.model.swagger.ResourceListing;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;

import com.wordnik.swagger.core.SwaggerSpec;
import org.restlet.routing.Router;

/**
 * Restlet that generates Swagger documentation in the format defined by the
 * swagger-spec project.<br>
 * It helps to generate the high level documentation for the whole API (set by
 * calling {@link #setApiInboundRoot(Application)} or
 * {@link #setApiInboundRoot(Restlet)} methods, and the documentation for each
 * resource.<br>
 * By default it instrospects the chain of Application's routers, filters,
 * restlet.<br>
 * Use the {@link JaxrsSwaggerSpecificationRestlet} restlet for Jax-RS
 * applications.
 *
 * <p>
 * Usage example:
 * <pre>
 * SwaggerSpecificationRestlet swagger1SpecificationRestlet = new SwaggerSpecificationRestlet();
 * swagger1SpecificationRestlet.setApiInboundRoot(this);
 * swagger1SpecificationRestlet.attach(baseRouter);
 * </pre>
 * </p>

 * @author Thierry Boileau
 * @see <a href="http://github.com/wordnik/swagger-ui">Swagger UI (github)</a>
 * @see <a href="http://petstore.swagger.wordnik.com">Petstore sample application of Swagger-UI</a>
 * @see <a href="http://helloreverb.com/developers/swagger">Swagger Developper page</a>
 */
public class SwaggerSpecificationRestlet extends Restlet {

    /** The root Restlet to describe. */
    Restlet apiInboundRoot;

    /** The version of the API. */
    private String apiVersion;

    /** The Application to describe. */
    Application application;

    /** The base path of the API. */
    private String basePath;

    /** The RWADef of the API. */
    private Definition definition;

    /** The root Restlet to describe. */
    private String jsonPath;

    /** The version of Swagger. */
    private String swaggerVersion = "1.2";

    /** Helper used to add CORS response headers */
    private CorsResponseHelper corsResponseHelper = new CorsResponseHelper();

    /**
     * Default constructor.<br>
     * Sets the {@link #swaggerVersion} to {@link SwaggerSpec#version()}.
     */
    public SwaggerSpecificationRestlet() {
        this(null);
    }

    /**
     * Constructor.<br>
     * Sets the {@link #swaggerVersion} to {@link SwaggerSpec#version()}.
     * 
     * @param context
     *            The context.
     */
    public SwaggerSpecificationRestlet(Context context) {
        super(context);
    }

    /**
     * Returns the Swagger documentation of a given resource, also known as
     * "API Declaration" in Swagger vocabulary.
     *
     * @param category
     *            The category of the resource to describe.
     * @return The representation of the API declaration.
     */
    public Representation getApiDeclaration(String category) {
        return new JacksonRepresentation<ApiDeclaration>(
                SwaggerTranslator.getApiDeclaration(category, getDefinition()));
    }

    /**
     * Returns the root Restlet for the given application.
     * 
     * @return The root Restlet for the given application.
     */
    public Restlet getApiInboundRoot() {
        if (apiInboundRoot == null) {
            if (application != null) {
                apiInboundRoot = application.getInboundRoot();
            }
        }

        return apiInboundRoot;
    }

    /**
     * Returns the API's version.
     * 
     * @return The API's version.
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Returns the base path of the API.
     * 
     * @return The base path of the API.
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Returns the application's definition.
     * 
     * @return The application's definition.
     */
    private synchronized Definition getDefinition() {
        if (definition == null) {
            synchronized (SwaggerSpecificationRestlet.class) {
                definition = ApplicationIntrospector.getDefinition(application);
                // This data seems necessary for Swagger codegen.
                if (definition.getVersion() == null) {
                    definition.setVersion("1.0");
                }
            }
        }

        return definition;
    }

    /**
     * Returns the base path of the API's resource.
     * 
     * @return The base path of the API's resource.
     */
    public String getJsonPath() {
        return jsonPath;
    }

    /**
     * Returns the representation of the whole resource listing of the
     * Application.
     *
     * @return The representation of the whole resource listing of the
     *         Application.
     */
    public Representation getResourceListing() {
        return new JacksonRepresentation<ResourceListing>(
                SwaggerTranslator.getResourcelisting(getDefinition()));
    }

    /**
     * Returns the version of Swagger used to generate this documentation.
     * 
     * @return The version of Swagger used to generate this documentation.
     */
    public String getSwaggerVersion() {
        return swaggerVersion;
    }

    @Override
    public void handle(Request request, Response response) {
        super.handle(request, response);

        // CORS support for Swagger-UI
        corsResponseHelper.addCorsResponseHeaderIfCorsRequest(request, response);

        if (Method.GET.equals(request.getMethod())) {
            Object resource = request.getAttributes().get("resource");

            if (resource instanceof String) {
                response.setEntity(getApiDeclaration((String) resource));
            } else {
                response.setEntity(getResourceListing());
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        }

    }

    /**
     * Defines two routes, one for the high level "Resource listing" (by default
     * "/api-docs"), and the other one for the "API declaration". The second
     * route is a sub-resource of the first one, defined with the path variable
     * "resource" (ie "/api-docs/{resource}").
     *
     * @param router
     *            The router on which defining the new route.
     *
     * @see #attach(org.restlet.routing.Router, String) to attach it with a custom path
     */
    public void attach(Router router) {
        attach(router, "/api-docs");
    }

    /**
     * Defines two routes, one for the high level "Resource listing", and the
     * other one for the "API declaration". The second route is a sub-resource
     * of the first one, defined with the path variable "resource".
     *
     * @param router
     *            The router on which defining the new route.
     * @param path
     *            The root path of the documentation Restlet.
     *
     * @see #attach(org.restlet.routing.Router) to attach it with the default path
     */
    public void attach(Router router, String path) {
        router.attach(path, this);
        router.attach(path + "/{resource}", this);
    }

    /**
     * Sets the root Restlet for the given application.
     * 
     * @param application
     *            The application.
     */
    public void setApiInboundRoot(Application application) {
        this.application = application;
    }

    /**
     * Sets the root Restlet for the given application.
     * 
     * @param apiInboundRoot
     *            The application's root Restlet.
     */
    public void setApiInboundRoot(Restlet apiInboundRoot) {
        this.apiInboundRoot = apiInboundRoot;
    }

    /**
     * Sets the API's version.
     * 
     * @param apiVersion
     *            The API version.
     */
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * Sets the base path of the API.
     * 
     * @param basePath
     *            The base path of the API
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * Sets the base path of the API's resource.
     * 
     * @param jsonPath
     *            The base path of the API's resource.
     */
    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    /**
     * Sets the version of Swagger used to generate this documentation.
     * 
     * @param swaggerVersion
     *            The version of Swagger.
     */
    public void setSwaggerVersion(String swaggerVersion) {
        this.swaggerVersion = swaggerVersion;
    }

}