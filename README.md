# UrbanCode Deploy Siebel Plugin
---
Note: This is not the plugin distributable! This is the source code. To find the installable plugin, go into the 'Releases' tab, and download a stable version.

### License
This plug-in is protected under the [Eclipse Public 1.0 License](http://www.eclipse.org/legal/epl-v10.html)

### Compatibility
	The IBM UrbanCode Deploy automation plug-in works with Siebel version 8.X.
	This plug-in requires version 6.1.1 or later of IBM UrbanCode Deploy.
    
### Installation
	The packaged zip is located in the releases folder. No special steps are required for installation.
	See Installing plug-ins in UrbanCode Deploy. Download this zip file if you wish to skip the 
	manual build step. Otherwise, download the entire Siebel-UCD and 
	run the "ant" command in the top level folder. This should compile the code and create
	a new distributable zip within the releases folder. Use this command if you wish to make
	your own changes to the plugin.

### History
    Version 4
        Community GitHub Release
 
### How to build the plugin from eclipse client:

1. Expand the Groovy project that you checked-out from example template.
2. Open build.xml file and execute it as an Ant Build operation (Run As -> Ant Build)
3. The built plugin is located at releases/UCDSiebelPlugin_v<version>.zip 

### How to build the plugin from command line:

1. Navigate to the base folder of the project through command line.
2. Make sure that there is build.xml file there, and then execute 'ant' command.
3. The built plugin is located at releases/UCDSiebelPlugin_vdev.zip 
Note: Edit the Build.xml to change the version 'dev' to a release number.
