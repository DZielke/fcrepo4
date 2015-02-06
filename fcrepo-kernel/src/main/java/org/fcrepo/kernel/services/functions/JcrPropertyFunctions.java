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
package org.fcrepo.kernel.services.functions;

import com.google.common.collect.Iterators;

import org.fcrepo.kernel.utils.UncheckedFunction;
import org.fcrepo.kernel.utils.UncheckedPredicate;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

import static javax.jcr.PropertyType.BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FROZEN_NODE;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public final class JcrPropertyFunctions {

    private JcrPropertyFunctions() {
    }

    /**
     * Translates a {@link javax.jcr.nodetype.NodeType} to its {@link String} name.
     */
    public static Function<NodeType, String> nodetype2name = UncheckedFunction.uncheck(NodeType::getName);

    /**
     * Translates a JCR {@link javax.jcr.Value} to its {@link String} expression.
     */
    public static Function<Value, String> value2string = UncheckedFunction.uncheck(Value::getString);

    /**
     * Constructs an {@link java.util.Iterator} of {@link javax.jcr.Value}s from any
     * {@link javax.jcr.Property}, multi-valued or not.
     */
    public static Function<Property, Iterator<Value>> property2values = UncheckedFunction.uncheck(p -> p.isMultiple()
            ? Iterators.forArray(p.getValues()) : Iterators.forArray(p.getValue()));

    /**
     * Check if a JCR property is a multivalued property or not
     */
    public static Predicate<Property> isMultipleValuedProperty = UncheckedPredicate.uncheck(p -> p.isMultiple());

    /**
     * Check if a JCR property is a binary jcr:data property
     */
    public static Predicate<Property> isBinaryContentProperty =
            UncheckedPredicate.uncheck(p -> p.getType() == BINARY && p.getName().equals(JCR_DATA));

    /**
     * Predicate for determining whether this {@link javax.jcr.Node} is a frozen node
     * (a part of the system version history).
     */
    public static Predicate<Node> isFrozen = UncheckedPredicate.uncheck(n -> n.isNodeType(FROZEN_NODE));

}
