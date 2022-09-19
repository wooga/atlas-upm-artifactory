package wooga.gradle.upm.artifactory.internal

import com.wooga.gradle.PropertyLookup

import java.util.function.Function

class DynamicPropertyLookup<T> implements Function<T, PropertyLookup> {

    final Function<T, List<String>> environmentKeysFactory
    final Function<T, List<String>> propertyKeysFactory
    final Function<T, Object> defaultValueFactory

    DynamicPropertyLookup(Function<T, List<String>> environmentKeys,
                          Function<T, List<String>> propertyKeys,
                          Function<T, Object> defaultValue) {
        this.environmentKeysFactory = environmentKeys
        this.propertyKeysFactory = propertyKeys
        this.defaultValueFactory = defaultValue
    }


    PropertyLookup apply(T filler) {
        resolve(filler)
    }

    PropertyLookup resolve(T filler) {
        def environmentKeys = environmentKeysFactory.apply(filler).collect{it.toString()}
        def propertyKeys = propertyKeysFactory.apply(filler).collect{it.toString()}
        def defaultValue = defaultValueFactory.apply(filler)

        return new PropertyLookup(environmentKeys, propertyKeys, defaultValue)
    }
}
