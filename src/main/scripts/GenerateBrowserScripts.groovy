/**
 * © Copyright IBM Corporation 2015.  
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;
import com.urbancode.air.ExitCodeException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

String myName="GenerateBrowserScripts";
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

def ServerHomeDirectory = props['SiebelHomeDirectory'];

def dirOffset = props['dirOffset'];
boolean isUnix=true
if(System.getProperty("os.name").startsWith("Win"))
{
	isUnix=false
	println(myName+": We are running in a Windows environment")
}
else
{
	//Here I assume that the syntax of AIX command is the same as Linux and other Unix commands for Siebel. Could be wrong though CAREFUL
	isUnix=true
	println(myName+": We are running in a Unix/Linux environment")
}
def workDir = new File("ucd_sieb_"+dateFormat.format(date));
def pathBeg = workDir.path+workDir.separator;
def script = ""
def check = ""
def scriptArgs = []
def checkArgs = []
if (isUnix)
{
	script = pathBeg + "scriptFile.sh"
	scriptArgs << "./" + script
	check = pathBeg + "checkFile.sh"
	checkArgs << "./" + check
}
else {
	script = pathBeg + "scriptFile.bat"
	scriptArgs << script
	check = pathBeg + "checkFile.bat"
	checkArgs << "./" + check
}
File scriptFile = new File(script)
File checkFile = new File(check)

boolean bToCheck = false;

def cleanUp = {
	println(myName+": Cleaning up.")
	if(scriptFile.exists())
	{
		if(!scriptFile.delete())
		{
			println(myName+": Could not delete script:["+scriptFile.path+"] please delete manually")
		}
	}
	if(checkFile.exists())
	{
		if(!checkFile.delete())
		{
			println(myName+": Could not delete check script:["+checkFile.path+"] please delete manually")
		}
	}
	if(workDir.exists())
	{
		if(!workDir.deleteDir())
		{
			println(myName+": Could not delete directory:["+workDir.path+"] please delete manually")
		}
	}
}

try
{
//	alias startapa='/usr/IBM/HTTPServer/bin/startapa'
//	alias startgate='. $HOME/.profile;. $GATEWAY_HOME/siebenv.sh ;$GATEWAY_HOME/bin/start_ns'
//	alias startsiebel='. $HOME/.profile;. $SIEBEL_HOME/siebenv.sh ;$SIEBEL_HOME/bin/start_server all'
//	alias stopapa='/usr/IBM/HTTPServer/bin/stopapa'
//	alias stopgate='. $HOME/.profile;. $GATEWAY_HOME/siebenv.sh ; $GATEWAY_HOME/bin/stop_ns'
//	alias stopsiebel='. $HOME/.profile; . $SIEBEL_HOME/siebenv.sh ; $SIEBEL_HOME/bin/stop_server all'
	
	println(myName+": Making directory ["+workDir.path+"]");
	workDir.mkdir();
	println scriptFile.absolutePath
	writer = new PrintWriter(scriptFile);
	envString=new String();
	commandString=new String();
	
	println(myName+": Cooking Generate Browser Scripts script ;)");
	if (isUnix)
	{
		envString=". \$HOME/.profile; SIEBEL_HOME="+ServerHomeDirectory+"; export SIEBEL_HOME; . \$SIEBEL_HOME/siebenv.sh;";
		commandString="\$SIEBEL_HOME/bin/genbscript \$SIEBEL_HOME/bin/enu/uagent.cfg \$SIEBEL_HOME/webmaster/enu";
		writer.println("#!/usr/bin/sh");
	}
	else
	{
		envString="set SIEBEL_HOME="+ServerHomeDirectory+"& call %SIEBEL_HOME%\\siebenv.bat";
		commandString="%SIEBEL_HOME%\\bin\\genbscript %SIEBEL_HOME%\\bin\\enu\\uagent.cfg %SIEBEL_HOME%\\webmaster\\enu";
	}
	println(myName+": "+envString);
	println(myName+": "+commandString);
	writer.println(envString);
	writer.println(commandString);
	writer.close();
	scriptFile.setExecutable(true, false);
}
catch (IOException e)
{
	cleanUp.call()
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace();
	System.exit(-1);
}

//example commandHelper
def ch = new CommandHelper(workDir);
int exitStatus=0;

exitStatus=ch.runCommand("Applying Action on Server", scriptArgs);
println(myName+": ExitStatus:["+exitStatus+"]")
if(exitStatus==1)
{
	println(myName+": ["+e.getMessage()+"]")
	System.exit(0)
}
else
{
	cleanUp.call()
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace()
	System.exit(-1)
}

/*
 * Let's leave the checking here for the time being, I have a feeling that 
 * I'll have to come up with some checking for this step.
 */
//if(bToCheck)
//{
//	if(isUnix)
//	{
//		println(myName+": Making check script[");
//		writer = new PrintWriter(checkFile);
//		writer.println(". \$HOME/.profile; SIEBEL_HOME="+ServerHomeDirectory+"; export SIEBEL_HOME; . \$SIEBEL_HOME/siebenv.sh;");
//		println("outp=`\$SIEBEL_HOME/bin/srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\"`");
//		writer.println("outp=`\$SIEBEL_HOME/bin/srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\"`");
//		writer.println("echo \$outp");
//		writer.println("mycount=`echo \$outp|grep -i "+sbServer.toString()+"|grep -c Running`");
//		writer.println("if [ \$mycount = 0 ]; then exit -1; else exit 0; fi");
//		writer.close();
//		checkFile.setExecutable(true,false);
//	}	
//	else
//	{
//		writer.println(myName+" Windows is not yet supported - please add code here TAG:CHECK")
//		//FILL IN REST OF SIEBEL ON WINDOWS THINGS. Don't have access to a windows Siebel server, so can't test it.
//	}
//}
//int rc=0;
//ch.ignoreExitValue(true);
//while(bToCheck)
//{
//	sleep(5000);
//	rc=ch.runCommand(dateFormat.format(date)+": "+myName+": Checking to see if Siebel Server has started", checkArgs);
//	println(myName+": check returned "+rc)
//	if(rc==0)
//	{
//		bToCheck=false;
//	}
//}
cleanUp.call()
//Set an output property
apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
