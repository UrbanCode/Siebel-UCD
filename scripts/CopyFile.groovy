/**
 * © Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

String myName="CopyFile";
/* This gets us the plugin tool helper. 
 * This assumes that args[0] is input props file and args[1] is output props file.
 * By default, this is true. If your plugin.xml looks like the example. 
 * Any arguments you wish to pass from the plugin.xml to this script that you don't want to 
 * pass through the step properties can be accessed using this argument syntax
 */
def apTool = new AirPluginTool(this.args[0], this.args[1]) 

/* Here we call getStepProperties() to get a Properties object that contains the step properties
 * provided by the user. 
 */
def props = apTool.getStepProperties();

/* This is how you retrieve properties from the object. You provide the "name" attribute of the 
 * <property> element 
 * 
 */
DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
Date date = new Date();

def ActionString = props['Action'];
def initLoc = props['InitialLocation'];
def finLoc = props['FinallLocation'];

def dirOffset = props['dirOffset'];

println System.getenv().toString()

try
{
	File fromFile = new File(initLoc);
	
	println(myName+": Action:["+ActionString+"] From:["+initLoc+"] To:["+finLoc+"]");
	if(!fromFile.isFile())
	{
		println(myName+": There is no file at location:["+initLoc+"]");
		System.exit(-1);
	}
	
	switch(ActionString)
	{
		case ~/Copy/:
			new File(finLoc.toString()).bytes = new File(initLoc.toString()).bytes
			break;
		case ~/Move/:
			if(!fromFile.renameTo(finLoc.toString()))
			{
				println(myName+": Could not move to:["+finLoc+"]");
			}
			break;
		default:
			System.out.println(myName+": invalid Action");
			System.exit(-1);
	}
}
catch(IOException e)
{
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace();
	System.exit(-1);
}

//Set an output property
apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
