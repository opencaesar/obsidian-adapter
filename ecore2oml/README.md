# Ecore2Oml

[ ![Download](https://api.bintray.com/packages/opencaesar/adapters/ecore2oml/images/download.svg) ](https://bintray.com/opencaesar/adapters/ecore2oml/_latestVersion)

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
--output-catalog-path | -o path/to/output/oml/catalog.xml [Required]
--optionsFilePath | -op path/to/options/file.json [Required]
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenLocal()
		maven { url 'https://dl.bintray.com/opencaesar/adapters' }
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.ecore:ecore2oml-gradle:+'
	}
}
task ecore2oml(type:io.opencaesar.ecore2oml.Ecore2OmlTask) {
	inputFolderPath = file('path/to/input/ecore/folder') [Required]
	outputCatalogPath = file('path/to/output/oml/catalog.xml') [Required]
	optionsFilePath = file('path/to/options/file.json') [Required]
}               
```

## Options File
The options file allows customization of the Ecore to OML mapping. It is a JSON document with the following sections:

```
{
	// This is used to change the IRI of the generated ontology
	"uriMapping" : [
		{ 
		  // original nsURI of the EPackage
		  "NSURI" : "http://www.eclipse.org/uml2/5.0.0/UML",
		  // new IRI of the corresponding ontology 
		  "value" : "http://www.eclipse.org/uml2/5.0.0/UML"
		}
	] ,
	// This is used to specify that some ecore.EClasses (and their subtypes) should be mapped to oml.RelationEntity 
	"relationships": [
		{
			// the IRI of the EClass
			"root": "http://www.eclipse.org/uml2/5.0.0/UML#DirectedRelationship",
			// the name of the EReference that is mapped to the source relation
			"source": "source",
			// the name of the EReference that is mapped to the target relation
			"target": "target",
			// the postfix that is added to the forward relation's name of all sub relation entities
			"forwardPostFix": "forward",
			// the postfix that is added to the reverse relation's name of all sub relation entities
			"reversePostFix": "reverse",
			// Overrides settings to particular sub relation entities
			"overrides": [
				{
					// IRI of a sub relation entity to override
					"iri": "http://www.eclipse.org/uml2/5.0.0/UML#PackageMerge",
					// Overriden name of the relation entity's forward relation
					"forwardName": "PackageMerge_CustomForward"
				},
				{
					// IRI of a sub relation entity to override
					"iri": "http://www.eclipse.org/uml2/5.0.0/UML#PackageImport",
					// Overriden name of the relation entity's forward relation
					"forwardName": "PackageImport_OtherCutomForward",
					// Overriden name of the relation entity's reverse relation
					"reverseName": "PackageMerge_CustomReverse"
				}
			]
		}
	],
	// Semantic flags (symmetic, asymmetric,reflexive,irreflexive, transitive) that can be turned on on the mapped relation entities 
	"semanticFlags_sample" : [
		{
		   // flags on a relation entity mapped from an EReference
		  "iri" : "http://www.eclipse.org/uml2/5.0.0/UML#activity_ownedGroup" ,
		  "onFlags" : ["asymmetric","reflexive","transitive"]
		},
		{
		  // flags on a relation entity mapped from an opposite EReference
		  "iri" : "http://www.eclipse.org/uml2/5.0.0/UML#activityEdge_activity" ,
		  "onFlags" : ["symmetric","irreflexive","transitive"]
		},
		{
		  // flags on a relation entity mapped from an EClass
		  "iri" : "http://www.eclipse.org/uml2/5.0.0/UML#TemplateBinding" ,
		  "onFlags" : ["asymmetric","reflexive","transitive"]
		}
	]
}
```