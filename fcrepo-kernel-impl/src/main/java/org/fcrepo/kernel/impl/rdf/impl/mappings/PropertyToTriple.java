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
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.fcrepo.kernel.impl.utils.Streams.fromIterator;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.base.Converter;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Triple;

/**
 * Utility for moving from JCR properties to RDF triples.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertyToTriple implements Function<Property, Stream<Triple>> {

    private static final PropertyConverter propertyConverter = new PropertyConverter();
    private final ValueConverter valueConverter;
    private final Converter<Node, Resource> graphSubjects;

    private static final Logger LOGGER = getLogger(PropertyToTriple.class);

    /**
     * Default constructor. We require a {@link Converter} in order to
     * construct the externally-meaningful RDF subjects of our triples.
     *
     * @param graphSubjects the graph subjects
     * @param session the session
     */
    public PropertyToTriple(final Session session, final Converter<Resource, FedoraResource> graphSubjects) {
        this.valueConverter = new ValueConverter(session, graphSubjects);
        this.graphSubjects = nodeToResource(graphSubjects);
    }

    @Override
    public Stream<Triple> apply(final Property p) {
        LOGGER.trace("Encountering property: {}", p);
        return fromIterator(new PropertyValueIterator(p)).map(v -> propertyvalue2triple(p, v));
    }

    /**
     * @param p A JCR {@link Property}
     * @param v The {@link Value} of that Property to use (in the case of
     *        multi-valued properties)  For single valued properties this
     *        must be that single value.
     * @return An RDF {@link Triple} representing that property.
     */
    private Triple propertyvalue2triple(final Property p, final Value v) {
        LOGGER.trace("Rendering triple for Property: {} with Value: {}", p, v);
        try {
            final Triple triple = create(graphSubjects.convert(p.getParent()).asNode(),
                    propertyConverter.convert(p).asNode(), convertObject(p, v));

            LOGGER.trace("Created triple: {} ", triple);
            return triple;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private com.hp.hpl.jena.graph.Node convertObject(final Property p, final Value v) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node object = valueConverter.convert(v).asNode();

        if (object.isLiteral()) {
            final String propertyName = p.getName();
            final int i = propertyName.indexOf('@');

            if (i > 0) {
                final LiteralLabel literal = object.getLiteral();
                final String datatypeURI = literal.getDatatypeURI();

                if (datatypeURI.isEmpty() || datatypeURI.equals(XSDstring.getURI())) {
                    final String lang = propertyName.substring(i + 1);
                    return createLiteral(literal.getLexicalForm(), lang, literal.getDatatype());
                }
            }
        }
        return object;
    }

}
