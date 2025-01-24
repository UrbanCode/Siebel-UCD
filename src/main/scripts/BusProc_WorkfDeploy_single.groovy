/**
 * (c) Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import org.codehaus.groovy.tools.shell.Shell;

import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.ibm.gr.urbancode.plugin.siebel.UCDSiebelUtility;
import com.ibm.gr.urbancode.plugin.siebel.AirPluginTool;
import com.urbancode.air.CommandHelper;
import com.siebel.data.*;

def apTool = new AirPluginTool(this.args[0], this.args[1]) 

def props = apTool.getStepProperties();

String myName="BusProc_WorkfDeploy_single";
def siebObjList = []

System.out.println(myName+": Is this an update:["+props['Update']+"]")
Properties mySubSystemProps=new Properties();

//Connect to Siebel and get correct BusComp
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper();

System.out.println(myName+": Connect");
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards";
	System.exit(-1);
}

System.out.println(myName+": myUCDStepObject object");
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility(mySiebJDB);

SiebelService  m_BusServ = null;
SiebelPropertySet m_FieldSetIN = null;
SiebelPropertySet m_FieldSetOUT = null;
String m_sMethod = null;
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	String busObjName = new String("Workflow Admin Service")
	m_BusServ=m_dataBean.getBusService(busObjName)
	if(m_BusServ==null)
	{
		return false
	}
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusServ)
	return true
}

def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}

m_sMethod=props['Action']

System.out.println(myName+": getObjects")
if(getObjects())
{
	System.out.println(myName+": SetProperties");
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	//Here we set the properties of the object we want to set.
	//We do this because we need to break out the step specific properties from the 
	//server specific properties in order to set only the correct ones
	//to the 
	Properties dProps=new Properties()
	dProps['FlowSearchSpec']=props['FlowSearchSpec']
//	dProps['DirPath']=props['DirPath']
//	dProps['Repository']=""

/*
 * 	DO NOT USE THESE PROPERTIES, they are for the IMPORT METHOD. Use instead the Specific IMPORT STEP created. It is much better.
  	dProps['Repository']=props['Repository'];
	dProps['ProjectName']=props['ProjectName'];
*/
	m_FieldSetIN=myUCDStepObject.createSiebelPropertySet(dProps)
	m_FieldSetOUT=myUCDStepObject.createSiebelPropertySet((Properties)null)
	if(m_FieldSetIN==null)
	{
		System.out.println(myName+": createFieldProperties failed, check log upwards.");
		exitGracefullyWithFail()
	}
	
	System.out.println(myName+": doAction")

	String lOutputPropName=new String();
	System.out.println(myName+": doAction Starting");
	
	//METHOD: Export ARGS: ExportDir(In), FlowSearchSpec(In), NumFlowExported(Out), Repository(In - optional)
	//METHOD: Activate ARGS: FlowSearchSpec(In), NumFlowActivated(Out)
	//METHOD: Deploy ARGS: FlowSearchSpec(In), NumFlowDeployed(Out)
	//METHOD: Import ARGS: FileSearchSpec(In - optional), ImportDir(In), NumFlowImported (Out), ProjectName (In), Repository (In - optional).
	//METHOD: DeleteDeploy ARGS: FlowSearchSpec(In), NumFlowDeployed(Out)
	switch(m_sMethod)
	{
		case ~/Export/:
			if(!props['DirPath'])
			{
				println"$myName: DirPath needs to be set for export - seems to be empty"
				exitGracefullyWithFail()
			}
			m_FieldSetIN.setProperty("ExportDir", props['DirPath']);
			m_FieldSetIN.setProperty("FlowSearchSpec", dProps['FlowSearchSpec']);
			lOutputPropName="NumFlowExported"
			break;
		case ~/Activate/:
			m_FieldSetIN.setProperty("FlowSearchSpec", dProps['FlowSearchSpec']);
			lOutputPropName="NumFlowActivated"
			break;
		case ~/Deploy/:
			m_FieldSetIN.setProperty("FlowSearchSpec", dProps['FlowSearchSpec']);
			lOutputPropName="NumFlowDeployed"
			break;
/*
* 	DO NOT USE THIS IMPORT METHOD. Use instead the Specific IMPORT STEP created. It is much better.
* 			case ~/Import/:

			m_FieldSetIN.setProperty("ImportDir",dProps['DirPath']);
			m_FieldSetIN.setProperty("Repository",dProps['Repository']);
			m_FieldSetIN.setProperty("ProjectName",dProps['ProjectName']);
			lOutputPropName="NumFlowImported"
			break;
*/
		case ~/DeleteDeploy/:
			m_FieldSetIN.setProperty("FlowSearchSpec", dProps['FlowSearchSpec']);
			lOutputPropName="NumFlowDeployed"
			break;
		default:
			System.out.println(myName+": doAction could not recognise method");
			exitGracefullyWithFail()
	}

	try
	{
		m_BusServ.invokeMethod(m_sMethod, m_FieldSetIN, m_FieldSetOUT);
		System.out.println(myName+": method Invoked with action:["+m_sMethod+"] acted on ["+m_FieldSetOUT.getProperty(lOutputPropName)+"] workflows");
	}
	catch (SiebelException e)
	{
		e.printStackTrace();
		System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		exitGracefullyWithFail()
	}
		
	System.out.println(myName+": doAction Ending");

	System.out.println(myName+": Finish")
	m_BusServ.release()
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
System.out.println(myName+": disconnect");
mySiebJDB.disconnect();

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
