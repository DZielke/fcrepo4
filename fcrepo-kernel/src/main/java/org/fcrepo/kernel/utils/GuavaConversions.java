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
package org.fcrepo.kernel.utils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;


/**
 * Conversions to Guava types from Java 8 types.
 *
 * @author ajs6f
 *
 */
@Deprecated
public class GuavaConversions {
    public static <T> Predicate<T> guavaPredicate(final java.util.function.Predicate<? super T> p) {
        return new Predicate<T>(){

            @Override
            public boolean apply(final T t) {
                return p.test(t);
            }};
    }

    public static <T,R> Function<T, R> guavaFunction(final java.util.function.Function<? super T, ? extends R> f) {
        return new Function<T, R>(){

            @Override
            public R apply(final T t) {
                return f.apply(t);
            }};
    }
}
