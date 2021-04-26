openEHR-OPT
===========

Groovy Support of openEHR Operational Templates for CaboLabs Grails/Java projects, with advanced command line tools.

This will be used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.


## Build

The build was tested with [Gradle 6.4.1](https://gradle.org/install/) installed from [SDKMAN!](https://sdkman.io/).

```shell
$ cd openEHR-OPT
$ gradle build
```

That will run the tests and build the file ./build/libs/opt.jar

For running tests, there are many options, examples below:

1. Run specific test case from a specific suite
2. Run all tests from a specific suite
3. Run all suites in a package
4. Run all tests

```shell
$ cd openEHR-OPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest.testCompleteOPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest
$ gradle test --tests com.cabolabs.openehr.opt*
$ gradle test
```

The test report in HTML will be under ./build/reports/tests/test/index.html


## Command Tools

### Generate UI for data input

```shell
$ opt uigen path_to_opt dest_folder
```

### Generate XML instances with random data

```shell
$ opt ingen path_to_opt dest_folder [amount] [version|composition|version_committer|tagged]
```

1. amount: defines how many XML instances will be generated
2. version: generates an instance of a VERSION object
3. composition: generates an instance of a COMPOSITION object
4. version_committer: generates an instance with the format required by the [EHRCommitter] to generate the UI and load data to test the [EHRServer].
5. tagged: generates a version instance with tags instead of data, useful to inject data from your app to commit to the [EHRServer]


### Validate XML or JSON instances against the schemas

Validate one instance:

```shell
$ opt inval path_to_xml_or_json_instance
```

Validate all instances in folder:

```shell
$ opt inval path_to_folder_with_xml_or_json_instances
```

> Note: if the folder contains JSON and XML, it will validate both with the correct schema, but the files should have .json or .xml extensions for the mixed validation to work OK.


In both cases, the output is "file IS VALID" or the list of validation errors if the file is not valid against the schemas.


## Requires Java 8 and Groovy 2.5.5+

> - - - - -
> Note: check the opt.sh/opt.bat files to see if the correct path to the groovy dependencies on your machine is set there.
> - - - - -



[EHRCommitter]: https://github.com/ppazos/EHRCommitter
[EHRServer]: https://github.com/ppazos/cabolabs-ehrserver


