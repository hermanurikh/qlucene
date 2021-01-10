# QLucene

An in-house engine to index and search for terms in given files.

### Table of contents

- [System requirements](#system-requirements)
- [Supported file formats](#supported-and-tested-file-formats)
- [Good to know](#good-to-know)

### System requirements
* JDK 11+

### Supported and tested file formats
* .txt
* .kt
* .java
* .properties

### Good to know
* Background monitoring is made with built-in Java library, where events come with delay up to a minute. Therefore:
    * please allow for up to a minute between modifying contents of monitored files and checking the visibility in index
    * integration tests checking background updates are disabled by default, they can be enabled with `-Dtest.profile=integration` build variable. Build time
    takes much more with it.
* the maximum number of files returned on search request is configurable with `reducer.size-based.max-size` parameter of `search.properties`
* the maximum depth when recursively registering directories is configurable by `directory.index.max-depth` parameter of `search.properties`
* deletion of root monitored directory itself is not supported. Inner directories and files of monitored directory can be deleted,
and the system will track it properly. However, if the directory itself which has been added to index directly is deleted, the behavior is 
 unspecified.