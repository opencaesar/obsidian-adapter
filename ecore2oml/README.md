# Ecore2Oml

[ ![Download](https://api.bintray.com/packages/opencaesar/ecore-adapter/ecore2oml/images/download.svg) ](https://bintray.com/opencaesar/ecore-adapter/ecore2oml/_latestVersion)

A tool that translates [Ecore](https://www.eclipse.org/modeling/emf/) models to [OML](https://opencaesar.github.io/oml-spec) vocabularies

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
--input-folder-path | -i path/to/ecore/folder [Required]
--output-catalog-path | -o path/to/oml/catalog.oml [Required]
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenLocal()
		maven { url 'https://dl.bintray.com/opencaesar/ecore-adapter' }
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.ecore:ecore2oml-gradle:+'
	}
}
task ecore2oml(type:io.opencaesar.ecore2oml.Ecore2OmlTask) {
	inputFolderPath = file('path/to/ecore/folder') [Required]
	outputCatalogPath = file('path/to/oml/catalog.xml') [Required]
}               
```