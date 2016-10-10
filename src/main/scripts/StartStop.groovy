/**
 * © Copyright IBM Corporation 2015, 2016.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

String myName="StartStop";

def apTool = new AirPluginTool(this.args[0], this.args[1]) 
def props = apTool.getStepProperties();

DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
Date date = new Date();

def ServerHomeDirectory = props['SiebelHomeDirectory'];
def ActionString = props['Action'];
def ServerType = props['ServerType'];
def SiebEnterprise = props['SiebelEnt'];
def gwServer = props['GatewayServer'];
def gwPort = props['GatewayPort'];
def sbUser = props['SiebelUser'];
def sbPass = props['SiebelPass']
///THIS NEEDS TO CHANGE
def sbServer = props['SiebelServer'];

def dirOffset = props['dirOffset'];
boolean isUnix=true
if(System.getProperty("os.name").startsWith("Win"))
{
	isUnix=false;
	println(myName+": We are running in a Windows environment")
}
else
{
	//Here I assume that the syntax of AIX command is the same as Linux and other Unix commands for Siebel. Could be wrong though CAREFUL
	isUnix=true;
	println(myName+": We are running in a Unix/Linux environment")
}
println System.getenv("HOME").toString()
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
	File outputFile = new File("output.txt")
	if(outputFile.exists())
	{
		if(!outputFile.delete())
		{
			println(myName+": Could not delete directory: 'output.txt' please delete manually")
		}
	}
	File resultFile = new File("result.txt")
	if(resultFile.exists())
	{
		if(!resultFile.delete())
		{
			println(myName+": Could not delete directory: 'result.txt' please delete manually")
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
	writer = new PrintWriter(scriptFile);
	envString=new String();
	commandString=new String();
	if (isUnix)
	{
		println(myName+": Server Type: ["+ServerType+"] in dir:["+ServerHomeDirectory+"]");
		switch (ServerType)
		{
			case ~/Siebel/:
				envString=". \$HOME/.profile; SIEBEL_HOME="+ServerHomeDirectory+"; export SIEBEL_HOME; . \$SIEBEL_HOME/siebenv.sh;";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Siebel Server start script");
					commandString="\$SIEBEL_HOME/bin/start_server all";
					bToCheck=true;
				}
				else
				{
					println(myName+": Cooking Siebel Server stop script");
					commandString="\$SIEBEL_HOME/bin/stop_server all";
				}
				break;
			case ~/Gateway/:
				envString=". \$HOME/.profile; GATEWAY_HOME="+ServerHomeDirectory+"; export GATEWAY_HOME; . \$GATEWAY_HOME/siebenv.sh;";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Gateway Server start script");
					commandString="\$GATEWAY_HOME/bin/start_ns";
				}
				else
				{
					println(myName+": Cooking Gateway Server stop script");
					commandString="\$GATEWAY_HOME/bin/stop_ns";
				}
				break;
			case ~/Web/:
				envString=". \$HOME/.profile; APACHE_HOME="+ServerHomeDirectory+"; export APACHE_HOME;";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Apache Server start script");
					commandString="\$APACHE_HOME/bin/startapa";
				}
				else
				{
					println(myName+": Cooking Apache Server stop script");
					commandString="\$APACHE_HOME/bin/stopapa";
				}
				break;
			default:
				System.out.println(myName+": invalid Action");
				writer.close();
				exit(-1);
		}
		println(myName+": "+envString);
		println(myName+": "+commandString);
		writer.println("#!/usr/bin/sh");
		writer.println(envString);
		writer.println(commandString);
	}
	else
	{
		println(myName+": Server Type: ["+ServerType+"] in dir:["+ServerHomeDirectory+"]");
		switch(ServerType)
		{
			case ~/Siebel/:
				envString="set SIEBEL_HOME="+ServerHomeDirectory+"& call %SIEBEL_HOME%\\siebenv.bat;";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Siebel Server start script");
					commandString="%SIEBEL_HOME%\\bin\\start_server all";
					bToCheck=true;
				}
				else
				{
					println(myName+": Cooking Siebel Server stop script");
					commandString="%SIEBEL_HOME%\\bin\\stop_server all";
				}
				break;
			case ~/Gateway/:
				envString="set GATEWAY_HOME="+ServerHomeDirectory+"& call %$GATEWAY_HOME%\\siebenv.bat";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Gateway Server start script");
					commandString="%GATEWAY_HOME%\\bin\\start_ns";
				}
				else
				{
					println(myName+": Cooking Gateway Server stop script");
					commandString="%GATEWAY_HOME%\\bin\\stop_ns";
				}
				break;
			case ~/Web/:
				envString="set APACHE_HOME="+ServerHomeDirectory+"";
				if(ActionString.toString().equalsIgnoreCase("start"))
				{
					println(myName+": Cooking Apache Server start script");
					commandString="%APACHE_HOME%\\bin\\startapa";
				}
				else
				{
					println(myName+": Cooking Apache Server stop script");
					commandString="%APACHE_HOME%\\bin\\stopapa";
				}
				break;
			default:
				System.out.println(myName+": invalid Action");
				writer.close();
				exit(-1);
		}
		println(myName+": "+envString);
		println(myName+": "+commandString);
		writer.println(envString);
		writer.println(commandString);
	}
	writer.close();
	scriptFile.setExecutable(true, false);
}
catch(IOException e)
{
	cleanUp.call()
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace();
	System.exit(-1);
}

//example commandHelper
def ch = new CommandHelper(workDir);
try
{
	ch.runCommand("Applying Action on Server", scriptArgs);
}
catch (Exception e)
{
	cleanUp.call()
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace();
	System.exit(-1);
}
if (bToCheck)
{
	println(myName+": Making check script[");
	writer = new PrintWriter(checkFile);
	if (isUnix)
	{
		writer.println(". \$HOME/.profile; SIEBEL_HOME="+ServerHomeDirectory+"; export SIEBEL_HOME; . \$SIEBEL_HOME/siebenv.sh;");
		println("outp=`\$SIEBEL_HOME/bin/srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\"`");
		writer.println("outp=`\$SIEBEL_HOME/bin/srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\"`");
		writer.println("echo \$outp");
		writer.println("mycount=`echo \$outp|grep -i "+sbServer.toString()+"|grep -c Running`");
		writer.println("if [ \$mycount = 0 ]; then exit -1; else exit 0; fi");
	}	
	else
	{
		writer.println("set SIEBEL_HOME="+ServerHomeDirectory+"& call %SIEBEL_HOME%\\siebenv.bat");
		println("outp=`%SIEBEL_HOME%\\bin\\srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\"`");
		writer.println("%SIEBEL_HOME%\\bin\\srvrmgr /g "+gwServer.toString()+":"+gwPort.toString()+" /e "+SiebEnterprise.toString()+" /u "+sbUser.toString()+" /p "+sbPass.toString()+" /c \"list servers\" > output.txt");
		writer.println("C:\\Windows\\System32\\FIND /I \""+sbServer.toString()+"\" output.txt | C:\\Windows\\System32\\FIND /I /C \"Running\" > result.txt")
		writer.println("set /p RESULT=<output.txt")
		writer.println("if 0==%RESULT% (exit -1) else (exit 0)")
		//writer.println("set mycount=`echo %outp%|C:\Windows\System32\FIND /I "+sbServer.toString()+"| C:\Windows\System32\FIND /C Running`");
		//writer.println("if [ %mycount% = 0 ]; then exit -1; else exit 0; fi");
	}
	writer.close();
	checkFile.setExecutable(true,false);
}
int rc=0;
ch.ignoreExitValue(true);
while (bToCheck)
{
	sleep(5000);
	rc=ch.runCommand(dateFormat.format(date)+": "+myName+": Checking to see if Siebel Server has started", checkArgs);
	println(myName+": check returned "+rc)
	if (rc==0)
	{
		bToCheck=false;
	}
}
cleanUp.call()
//Set an output property
apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
