# QLucene

An in-house engine to index and search for terms in given files.

### Table of contents

- [What is QLucene?](#what-is-qlucene)
- [System requirements](#system-requirements)
- [How do I run it?](#what-is-qlucene)
- [Supported file formats](#supported-and-tested-file-formats)
- [Good to know](#good-to-know)

### What is QLucene?
QLucene is an indexing and searching application. Its API allows adding directories and files to the index, and later searching
for the files which contain the given term. It monitors files and directories which had been added to index and updates the index
if the former are changed.

In particular, current implementation provides the following functionality:
* adding files or directories to index. Directories are added recursively, that means, adding top-level directory results in nested directories
also being tracked - with a limitation of depth configurable by `directory.index.max-depth` parameter of `search.properties`;
* searching for files by given words or sentences;
* when a previously added to index file or directory is changed (e.g. any file content change, or addition/removal of file/directory), corresponding
changes get propagated to index;
* concurrent searches/updates are supported.

### System requirements
* JDK 11+

### How do I run it?
* [Preferred way] Just run a `QLuceneApplication::main` function from your favorite IDE~~A~~ and that should be it
* [CI way] 
    * *nix: `./gradlew build` and `java -jar build/libs/qlucene-0.0.1-SNAPSHOT.jar`
    * Windows: `gradlew.bat build` and `java -jar build/libs/qlucene-0.0.1-SNAPSHOT.jar`

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