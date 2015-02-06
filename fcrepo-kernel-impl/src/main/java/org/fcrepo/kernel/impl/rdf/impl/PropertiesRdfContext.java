/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.rdf.impl;

import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalProperty;
import static org.fcrepo.kernel.utils.Streams.fromIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.stream.Stream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;

import org.slf4j.Logger;

/**
 * {@link NodeRdfContext} for RDF that derives from JCR properties on a Resource
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertiesRdfContext extends NodeRdfContext {

    private final PropertyToTriple property2triple;

    private static final Logger LOGGER = getLogger(PropertiesRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */

    public PropertiesRdfContext(final FedoraResource resource,
                                final IdentifierConverter<Resource, FedoraResource> idTranslator)
        throws RepositoryException {
        super(resource, idTranslator);
        this.property2triple = new PropertyToTriple(resource.getNode().getSession(), idTranslator);
        LOGGER.trace("Creating triples for node: {}", resource());
        final Iterator<Property> propertiesIterator = resource().getNode().getProperties();
        final Stream<Property> properties = fromIterator(propertiesIterator).filter(isInternalProperty.negate());
        concat(properties.flatMap(property2triple));
    }
}
