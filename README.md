# SubDisc: Subgroup Discovery

SubDisc is a Data Mining tool for discovering local patterns in data. SubDisc features a generic Subgroup Discovery
algorithm that can be configured in many ways, in order to implement various forms of local pattern discovery. The tool
can deal with a range of data types, both for the input attributes as well as the target attributes, including nominal, 
numeric and binary. 

A unique feature of SubDisc is its ability to deal with a range of Subgroup Discovery settings, determined by the type
and number of target attributes. Where regular SD algorithms only consider a single target attribute, nominal or 
sometimes numeric, Cortana is able to deal with targets consisting of multiple attributes, in a setting called 
Exceptional Model Mining.

![screenshots](manual/cortana-screenshots.png)

## Features
* Generic parameterized Subgroup Discovery algorithm.
* Multiple data types supported.
* Implemented in Java, so works on all major platforms, including Windows, Linux and Mac OS.
* Works on propositional (tabular) data from flat files, .TXT or .ARFF.
* Includes Exceptional Model Mining settings.
* Statistical validation of mining results.
* Graphical presentation of results, such as ROC curves, scatter plots, and exceptional models.
* Additional bioinformatics module for literature-based gene set enrichment (see bioinformatics below).
* Free binary version and open-source access. 
* Wrapper available for R (https://github.com/SubDisc/rSubDisc) and Python (soon)

The code is compatible with Java 15. 

## To use

1. Either download the last released version jar file (https://github.com/SubDisc/SubDisc/releases/) or build it yourself (below).
2. Double-click on the .jar file or use java cli (ex.: `java --jar subdisc-gui-2.1094.jar`).

The interface should appear, and you are ready to open a data file and discover subgroups!

## How to build
1. Clone the repository: `git clone https://github.com/SubDisc/SubDisc.git`
2. Use maven to assemble the .jar file: `mvn package`
3. The .jar file is created in `./target` and named something like `subdisc-gui-2.1094.jar`. 
