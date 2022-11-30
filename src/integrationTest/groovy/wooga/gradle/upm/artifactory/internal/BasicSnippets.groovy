package wooga.gradle.upm.artifactory.internal

import com.wooga.gradle.test.PropertyUtils

class BasicSnippets implements BasicSnippetsTrait {}

trait BasicSnippetsTrait {

    static String wrap(Object rawValue, Class type) {
        return PropertyUtils.wrapValueBasedOnType(rawValue, type)
    }

    static String wrap(Object rawValue, String type) {
        return PropertyUtils.wrapValueBasedOnType(rawValue, type)
    }
}
