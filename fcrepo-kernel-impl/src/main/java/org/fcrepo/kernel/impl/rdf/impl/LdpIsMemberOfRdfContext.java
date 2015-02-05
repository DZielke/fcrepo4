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

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.impl.utils.Streams.fromIterator;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/7/14
 */
public class LdpIsMemberOfRdfContext extends NodeRdfContext {
    private final ValueConverter valueConverter;

    private static final List<Integer> PROPERTY_TYPES = asList(REFERENCE, WEAKREFERENCE, PATH);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception
     */
    public LdpIsMemberOfRdfContext(final FedoraResource resource,
                                   final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        this.valueConverter = new ValueConverter(session(), translator());
        final FedoraResource container = resource.getContainer();

        if (container != null &&
                (container.hasType(LDP_DIRECT_CONTAINER) || container.hasType(LDP_INDIRECT_CONTAINER)) &&
                container.hasProperty(LDP_IS_MEMBER_OF_RELATION)) {
            concatIsMemberOfRelation(container);
        }
    }

    private void concatIsMemberOfRelation(final FedoraResource container) throws RepositoryException {
        final Property property = container.getProperty(LDP_IS_MEMBER_OF_RELATION);

        final Resource memberRelation = createResource(property.getString());
        final Resource membershipResource = getMemberResource(container);

        if (membershipResource == null) {
            return;
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return;
            }
        } else {
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }

        if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {
            concat(create(subject(), memberRelation.asNode(), membershipResource.asNode()));
        } else if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            final String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(), createResource
                    (insertedContainerProperty), null);

            if (!resource().hasProperty(insertedContentProperty)) {
                return;
            }

            final PropertyValueIterator values
                    = new PropertyValueIterator(resource().getProperty(insertedContentProperty));

            final Stream<RDFNode> insertedContentRelations =
                    fromIterator(transform(values, valueConverter)).filter(
                            n -> n.isURIResource() && translator().inDomain(n.asResource()));

            concat(insertedContentRelations.map(n -> create(n.asNode(), memberRelation.asNode(), membershipResource
                    .asNode())));

        }
    }

    /**
     * Get the membership resource relation asserted by the container
     * @param parent
     * @return
     * @throws RepositoryException
     */
    private Resource getMemberResource(final FedoraResource parent) throws RepositoryException {
        if (parent.hasProperty(LDP_MEMBER_RESOURCE)) {
            final Property memberResource = parent.getProperty(LDP_MEMBER_RESOURCE);
            final int type = memberResource.getType();
            if (PROPERTY_TYPES.contains(type)) {
                return nodeConverter().convert(memberResource.getNode());
            }
            return createResource(memberResource.getString());
        }
        return translator().reverse().convert(parent);
    }
}
