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

import java.util.stream.Stream;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import static com.hp.hpl.jena.graph.Triple.create;
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.FedoraJcrTypes.VERSIONABLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/16/14
 */
public class ParentRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ParentRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public ParentRdfContext(final FedoraResource resource,
                            final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);
    }

    @Override
    public Stream<Triple> applyThrows(final javax.jcr.Node node) throws RepositoryException {
        if (node.getDepth() > 0 &&
                (!resource().isFrozenResource() || !resource().getUnfrozenResource().hasType(VERSIONABLE))) {
            LOGGER.trace("Determined that this resource has an appropriate parent.");
            final FedoraResource container = resource().getContainer();
            final Node containerSubject = translator().reverse().convert(container).asNode();
            return Stream.of(create(topic(), HAS_PARENT.asNode(), containerSubject));
        }
        return empty();
    }
}
