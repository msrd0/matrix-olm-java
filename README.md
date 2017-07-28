# matrix-olm-java [![GitLab CI Build Status](https://gitlab.com/mextrix/matrix-olm-java/badges/master/build.svg)](https://gitlab.com/mextrix/matrix-olm-java/pipelines) [![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://apache.org/licenses/LICENSE-2.0.html)

A JNI port of the OLM library from matrix. It is a fork of their android build with changed dependencies so that one
can use it for every Java project, not necessarily targetting Android.

## Gradle

```gradle
repositories {
	maven { url "https://msrd0.duckdns.org/artifactory/gradle" }
}

dependencies {
	compile "msrd0.matrix:matrix-olm-java:+"
}
```
