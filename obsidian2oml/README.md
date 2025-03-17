# Obsidian2Oml

[![Release](https://img.shields.io/github/v/tag/opencaesar/obsidian-adapter?label=release)](https://github.com/opencaesar/obsidian-adapter/releases/latest)

A tool that translates [Obsidian](https://obsidian.md/) files to [OML](https://opencaesar.github.io/oml) descriptions

## Run as CLI

MacOS/Linux

```
    ./gradlew obsidian2oml:run --args="..."
```
Windows

```
    gradlew.bat obsidian2oml:run --args="..."
```

Args

```
--input-folder-path | -i path/to/input/ecore/folder [Required]
--output-folder-path | -o path/to/output/oml/folder [Required]
--referenced-ecore-path | -r path/to/referenced/ecore/file [Optional]
--input-file-extension | -ie Extension of input file [Optional, ecore/xcore by default]
--output-file-extension | -oe Extension of output file (Optional, oml by default, other options omlxmi and omljson)
--namespace-map | -ns Mapping of old namespace prefix to new namespace prefix (Optional, syntax is oldNsPrefix=newNsPrefix)
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
		classpath 'io.opencaesar.adapters:obsidian2oml-gradle:+'
	}
}
task obsidian2oml(type:io.opencaesar.obsidian2oml.Obsidian2OmlTask) {
	inputFolderPath = file('path/to/input/ecore/folder') // Required
	outputFolderPath = file('path/to/output/oml/folder') // Required
	referencedEcorePaths = [ file('path/to/options/file.json') ] // Optional
	inputFileExtensions = ['ecore', 'xcore'] // Optional
	outputFileExtension = 'oml' // Optional (other options, omlxmi or omljson)
	namespaceMap = [ 'oldNsPrefix=newNsPrefix' ] // Optional
}               
```