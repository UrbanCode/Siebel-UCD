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

String myName="ImportRepository";

def apTool = new AirPluginTool(this.args[0], this.args[1]) 
def props = apTool.getStepProperties();

DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
Date date = new Date();

def SiebelHomeDirectory = props['SiebelHomeDirectory'];
def SiebelODBCDataSource = props['SiebelODBCDataSource'];
def SiebelDBTableOwner = props['SiebelDBTableOwner'];
def SiebelAdminUsername = props['SiebelUser'];
def SiebelAdminPassword = props['SiebelPass'];
def rpdFullPath = props['SiebelRepPath'];
def repository = props['SiebelRepName'];

def dirOffset = props['dirOffset'];
if (System.getProperty("os.name").startsWith("Win"))
{
	isUnix=false;
}
else
{
	isUnix=true;
}

def workDir = new File(System.getProperty("java.io.tmpdir").toString()+System.getProperty("file.separator").toString()+"ucd_sieb_"+dateFormat.format(date));
def script = workDir.path+workDir.separator;
def args = []
if (isUnix)
{
	script += "scriptFile.sh"
	args << "./" + script
}
else {
	script += "scriptFile.bat"
	args << script
}
File scriptFile = new File(script)

// Add all commands to the script file
try
{
	println(myName+": Making directory ["+workDir.path+"]");
	workDir.mkdir();
	writer = new PrintWriter(scriptFile);
	if (isUnix)
	{
		writer.println("SIEBEL_HOME="+SiebelHomeDirectory+"; export SIEBEL_HOME");
		writer.println(". \$SIEBEL_HOME/siebenv.sh");
		//./repimexp /A I /U sadmin /P <sadmin password> /C <ODBC_NAME> /D SIEBEL /R "Siebel Repository 20150329" 
		//	/F /appl/sblappl1/siebel/temp/siebel_rep.data /Z 5000 /H 5000 /G ALL
		String sComLine=new String();
		sComLine="\$SIEBEL_HOME/bin/repimexp /A /I /U "+SiebelAdminUsername+" /P "+SiebelAdminPassword+" /C "+SiebelODBCDataSource;
		sComLine=sComLine+" /D "+SiebelDBTableOwner+" /R \""+repository+"\" /F "+rpdFullPath+" /Z 5000 /H 5000 /G ALL";
		writer.println(sComLine);
	}
	else
	{
		writer.println("set SIEBEL_HOME="+SiebelHomeDirectory);
		writer.println("call %SIEBEL_HOME%\\siebenv.bat");
		String sComLine=new String();
		sComLine="%SIEBEL_HOME%\\bin\\repimexp.exe /A /I /U "+SiebelAdminUsername+" /P "+SiebelAdminPassword+" /C "+SiebelODBCDataSource;
		sComLine=sComLine+" /D "+SiebelDBTableOwner+" /R \""+repository+"\" /F "+rpdFullPath+" /Z 5000 /H 5000 /G ALL";
		writer.println(sComLine);
	}
	writer.close();
	scriptFile.setExecutable(true, false);
}
catch (IOException e)
{
	println(myName+": ["+e.getMessage()+"]");
	e.printStackTrace();
	exit(-1);
}

//example commandHelper
def ch = new CommandHelper(workDir);
ch.runCommand("Importing Siebel repository", args);
scriptFile.deleteOnExit();
workDir.deleteOnExit();

//Set an output property
apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
