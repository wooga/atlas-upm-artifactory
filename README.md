atlas-upm-artifactory
===============

![Wooga Internal](https://img.shields.io/badge/wooga-internal-lightgray.svg?style=flat-square)
[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.wdk--unity-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.upm-artifactory)
[![Build Status](https://img.shields.io/travis/wooga/atlas-unity/master.svg?style=flat-square)](https://travis-ci.org/wooga/atlas-upm-artifactory)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-upm-artifactory/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-upm-artifactory.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-upm-artifactory.svg?style=flat-square)]()

This plugin provides tasks to package and publish UPM libraries on artifactory

## Documentation

- [API docs](https://wooga.github.io/atlas-upm-artifactory/docs/api/)
- [Release Notes](RELEASE_NOTES.md)

Gradle and Java Compatibility
=============================

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version | Works       |
| :------------- | :---------: |
| <= 2.13        | ![no]       |
| 2.14           | ![yes]      |
| 3.0            | ![yes]      |
| 3.1            | ![yes]      |
| 3.2            | ![yes]      |
| 3.4            | ![yes]      |
| 3.4.1          | ![yes]      |
| 3.5            | ![yes]      |
| 3.5.1          | ![yes]      |
| 4.0            | ![yes]      |
| 4.1            | ![yes]      |
| 4.2            | ![yes]      |
| 4.3            | ![yes]      |
| 4.4            | ![yes]      |
| 4.5            | ![yes]      |
| 4.6            | ![yes]      |
| 4.7            | ![yes]      |
| 4.8            | ![yes]      |
| 4.9            | ![yes]      |
| 4.10           | ![yes]      |


Usage
=============================

`UPMArtifactoryPlugin` supports a diversity of configurations, on many upm projects. 
Each upm project needs to declare at least their directory inside of a named block, as shown below. 
Please note that you can only have a single repository that will be used on all declared projects. 
```groovy
upm {
    repository = //String, name of upm repository previously configured in publishing plugin. Mandatory
    username = //String, username of target private UPM repository.  Can be empty
    password = //String, password of target private UPM repository. Can be empty
    projectName {
        packageDirectory = //Directory, base folder of desired UPM package, mandatory.
        version = //String, version of the published package. Is not validated in any way. Defaults to `project.version`
        generateMetaFiles = //boolean, set to true to force metafile generation. 
    }
    otherProjectName {
        //...
    }
}
``` 

As UPM needs its packages to have Unity metafiles inside of them, the project tries to check if the package needs to regenerate its metafiles, 
and if true, generates them. If somehow metafiles aren't still properly present in the final project, 
the generation can be forced by setting the `upm.generateMetafiles` property to `true`. Please note that setting this 
to `false` doesn't block automatic metafile generation.

UPM repositories should be configured inside the `publishing` block, in a similar way to 
paket repositories, as shown below. Those repositories can be then referenced by their name in the upm extension `repository` configuration.

```groovy
publishing {
    repositories {
        upm {
            name "snapshot"
            url "https://artifactory.repo/mynpmrepo/upm-snapshot"
        }

        upm {
            name "rc"
            url "https://artifactory.repo/mynpmrepo/upm-rc"
        }

        upm {
            name "final"
            url "https://artifactory.repo/mynpmrepo/upm-release"
        }
    }
}
```

The `credentials` block is supported as well, so you can do as below. The credentials are set for that specific repository. 
```groovy
publishing {
    repositories {
        upm {
            name "myrepo"
            url "https://artifactory.repo/mynpmrepo/upm-myrepo"
            credentials {
                username = //String, repository username 
                password =  //String, repository password
            }
        }
   ...
    }
}
```
The UPM-related information in the `publishing` block is translated into the `upm` extension, so the 
username and password for your selected repositories are available through the `upm.username` and `upm.password` properties. 
Those properties can also be used to set the `username` & `password` for the repository specified in the `repository` property.

You can also check repository-specific information for your selected repository using the `upm.selectedUPMRepository` provider.

Packaging tasks (`GenerateUpmPackage`) are created for each project, named with the format `{projectName}UpmPack`. 
Those tasks create a tarball from the given upm project directory. 
Then this tarball is transformed into a gradle artifact named `{projectName}UpmArtifact`, 
which is stored in a gradle configuration named `{projectName}Upm`. The artifact is also stored in a 
Publishing plugin publication equally named `{projectName}Upm`, which is published to the specified repository. 

The UPM publishing is connected to the Artifactory and Publishing plugins, so just running the `publish` task
should be enough to publish a package, given that all mandatory requirements are set 
(a UPM repository and a corresponding repository name configured into `upm.repository`).





Development
===========
[Code of Conduct](docs/Code-of-conduct.md)

LICENSE
=======

Copyright 2017 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[yes]:                  http://atlas-resources.wooga.com/icons/icon_check.svg "yes"
[no]:                   http://atlas-resources.wooga.com/icons/icon_uncheck.svg "no"
