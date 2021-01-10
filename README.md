# QLucene

An in-house engine to index and search for terms in given files.

### Table of contents

- [What is QLucene?](#what-is-qlucene)
- [System requirements](#system-requirements)
- [How do I run it?](#how-do-i-run-it)
- [How do I use it?](#how-do-i-use-it)
    - [File addition to index](#1-file-addition-to-index)
    - [Directory addition to index](#2-directory-addition-to-index)
    - [Searching for files containing given word](#3-searching-for-files-containing-given-word)
    - [Searching for files containing given sentence](#4-searching-for-files-containing-given-sentence)
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
#### Preferred way - IDE
Just run a `QLuceneApplication::main` function from your favorite IDE~~A~~ and that should be it.
#### CI way
#####  For *nix
 ```
 ./gradlew build
java -jar build/libs/qlucene-0.0.1-SNAPSHOT.jar
```
#####  Windows
```
gradlew.bat build
java -jar build/libs/qlucene-0.0.1-SNAPSHOT.jar
```
    
> Make sure you have JDK 11 or higher on path to build the application.
    
### How do I use it?
`UserAPI.kt` has the API exposed via REST. You can check there for exact input parameters which are expected. 

Below are some examples.
#### 1. File addition to index
Hit the POST `/add/` endpoint with request parameter `path` set to necessary file URI to add. 

Example for MacOS:

<small>Request:</small>
```
curl --data "path=src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt" http://localhost:8077/add/
```
<small>Response:</small>
```
{"message":"Successfully registered file: src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt"}
```
#### 2. Directory addition to index
The same as above, just specify the parameter `path` to point to a valid directory.

Example for MacOS:

<small>Request:</small>
```
curl --data "path=src/test/resources/testfiles" http://localhost:8077/add/
```
<small>Response:</small>
```
{"message":"Successfully registered directory: src/test/resources/testfiles"}
```
#### 3. Searching for files containing given word 
Hit the GET `/search/word/{token}` endpoint, where `{token}` is the desired word.

Example for MacOS:

<small>Request:</small>
```
curl -i http://localhost:8077/search/word/august
```
<small>Response:</small>
```
["src/test/resources/testfiles/rootdir2/simpleFile3.txt","src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt"]
```
#### 4. Searching for files containing given sentence
Hit the GET `/search/sentence/{token}` endpoint, where `{token}` is the desired sentence. Make sure that it is properly encoded as part of URL.
Example for MacOS:

<small>Request:</small>
```
curl -i http://localhost:8077/search/sentence/Simple%20sentence%202...
```
<small>Response:</small>
```
["src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt"]
```
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