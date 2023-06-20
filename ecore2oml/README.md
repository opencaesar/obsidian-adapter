# Ecore2Oml

[![Release](https://img.shields.io/github/v/tag/opencaesar/ecore-adapter?label=release)](https://github.com/opencaesar/ecore-adapter/releases/latest)

A tool that translates [Ecore](https://www.eclipse.org/modeling/emf/) models to [OML](https://opencaesar.github.io/oml) vocabularies

## Run as CLI

MacOS/Linux

```
    ./gradlew ecore2oml:run --args="..."
```
Windows

```
    gradlew.bat ecore2oml:run --args="..."
```

Args

```
--input-folder-path | -i path/to/input/ecore/folder [Required]
--output-folder-path | -o path/to/output/oml/folder [Required]
--referenced-ecore-path | -r path/to/referenced/ecore/file [Optional]
--input-file-extension | -ie Extension of input file [Optional, ecore/xcore by default]
--output-file-extension | -oe Extension of output file (Optional, oml by default, other options omlxmi and omljson)
--debug | -d Shows debug statements
--help | -h Shows help
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.adapters:ecore2oml-gradle:+'
	}
}
task ecore2oml(type:io.opencaesar.ecore2oml.Ecore2OmlTask) {
	inputFolderPath = file('path/to/input/ecore/folder') // Required
	outputFolderPath = file('path/to/output/oml/folder') // Required
	referencedEcorePaths = [ file('path/to/options/file.json') ] // Optional
	inputFileExtensions = ['ecore', 'xcore'] // Optional
	outputFileExtension = 'oml' // Optional (other options, omlxmi or omljson)
}               
```