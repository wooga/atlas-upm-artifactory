/*
 * Copyright 2022 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.upm.artifactory.internal

import org.gradle.api.internal.provider.DefaultMapProperty
import org.gradle.api.internal.provider.DefaultPropertyFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider

class MapPropertyWithRightDefault<K, V> extends DefaultMapProperty<K, V> {

    MapProperty<K, V> defaultValue;

    MapPropertyWithRightDefault(ObjectFactory objects, Class<K> keyType, Class<V> valueType) {
        super(((DefaultPropertyFactory)objects.propertyFactory).propertyHost, keyType, valueType)
        defaultValue = objects.mapProperty(keyType, valueType)
    }

    @Override
    void set(Provider<? extends Map<? extends K, ? extends V>> provider) {
        super.set(provider.orElse(defaultValue))
    }

    @Override
    MapProperty<K, V> value(Provider<? extends Map<? extends K, ? extends V>> provider) {
        return super.value(provider.orElse(defaultValue))
    }

    @Override
    MapProperty<K, V> convention(Map<? extends K, ? extends V> valueProvider) {
        defaultValue.set(valueProvider as Map)
        return this
    }

    @Override
    MapProperty<K, V> convention(Provider<? extends Map<? extends K, ? extends V>> valueProvider) {
        defaultValue.set(valueProvider as Provider)
        return this
    }
}
