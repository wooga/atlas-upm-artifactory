/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.upm.artifactory.internal.repository

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor

import java.util.regex.Pattern

class UPMRepository implements UPMArtifactRepository {
    final static Pattern BASE_URL_REGEX = Pattern.compile("(.*)\\/.+")
    private String url
    private String baseUrl
    private String repositoryKey
    //implements getName and setName
    String name
    //implements getCredentials
    RepositoryCredentials credentials = new RepositoryCredentials()
    
    def name(String name) {
        setName(name)
    }

    def url(String url) {
        setUrl(url)
    }

    @Override
    String getUrl() {
        return url
    }

    void setUrl(String url) {
        def baseUrlMatcher = BASE_URL_REGEX.matcher(url)
        if(baseUrlMatcher.matches()) {
            this.baseUrl = baseUrlMatcher.group(1)
            this.repositoryKey = url.replace(baseUrl, "").replace("/", "")
            this.url = url
        } else {
            throw new IllegalArgumentException("The given URL was not a valid artifactory URL (http(s)://artifactoryhost/artifactory/your-repo)")
        }
    }

    @Override
    String getBaseUrl() {
        return baseUrl
    }

    @Override
    String getRepositoryKey() {
        return repositoryKey
    }

    @Override
    void credentials(Closure credsConfig) {
        credentials.with(credsConfig)
    }

    @Override
    void content(Action<? super RepositoryContentDescriptor> configureAction) {

    }

    
}
