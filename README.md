# IBM UrbanCode Deploy Siebel Plugin [![Build Status](https://travis-ci.org/IBM-UrbanCode/Siebel-UCD.svg?branch=master)](https://travis-ci.org/IBM-UrbanCode/Siebel-UCD)
---
Note: This is not the plugin distributable! This is the source code. To find the installable plugin, go into the 'Releases' tab, and download a stable version.

### License
This plugin is protected under the [Eclipse Public 1.0 License](http://www.eclipse.org/legal/epl-v10.html)

### Compatibility
	The IBM UrbanCode Deploy automation plugin works with Siebel version 8.X.
	This plugin requires version 6.1.1 or later of IBM UrbanCode Deploy.

### Installation
	The packaged zip is located in the releases folder. No special steps are required for installation.
	See Installing plugins in UrbanCode Deploy. Download this zip file if you wish to skip the
	manual build step. Otherwise, download the entire Siebel-UCD and
	run the "ant" command in the top level folder. This should compile the code and create
	a new distributable zip within the releases folder. Use this command if you wish to make
	your own changes to the plugin.

    Note: Two Siebel Data Bean specific jars are required to run this plugin: Siebel.jar and SiebelJI_enu.jar.
    These can be downloaded from Oracle's website. To install the jars, follow these steps:
    first extract the plugin, then copy the jars into the /lib directory, re-zip the extracted plugin files,
    and install as normal into UCD. These jars must be named as stated above. Directions to find the
    jars on your local Siebel instance:
    http://www.ibm.com/support/knowledgecenter/SSEP7J_10.2.0/com.ibm.swg.ba.cognos.vvm_user_guide.10.2.0.doc/t_vvm_user_siebel_jars_install.html

### History
    Version 5
        Community GitHub Release
    Version 4
        Community GitHub Release

### How to build the plugin from eclipse client:

1. Expand the Groovy project that you checked-out from example template.
2. Open build.xml file and execute it as an Ant Build operation (Run As -> Ant Build)
3. The built plugin is located at releases/Siebel-UCD-v<version>.dev.zip

### How to build the plugin from command line:

1. Navigate to the base folder of the project through command line.
2. Make sure that there is build.xml file there, and then execute 'ant' command.
3. The built plugin is located at releases/Siebel-UCD-v<version>.dev.zip
