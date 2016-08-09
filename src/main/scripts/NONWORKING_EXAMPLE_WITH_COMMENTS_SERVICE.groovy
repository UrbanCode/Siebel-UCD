/*
 * Here is the default imports that all scripts should have as a minimum
 */
import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.ibm.gr.urbancode.plugin.siebel.UCDSiebelUtility;
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;
import com.siebel.data.*;

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
def props = apTool.getStepProperties()

/*
 * I have set up myName to be able to distinguish easily the steps from the logs
 */
String myName="BusProc_WorkfImport";
/*
 * This is a little list in which we add all the Siebel components we need to clean
 * in an error condition.
 */
def siebObjList = []

//Create a new Siebel Java Data Bean Wrapper.
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper()

/*
 * Here we connect to the Siebel Connection Broker Server. We connect to the 
 * EAIObjMgr_enu Object Manager, which seems to be the most well behaved for Siebel.
 * We pick up all the options from the Step options. 
 */
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards"
	System.exit(-1)
}

/*
 * Let's create a utilitity object. We pass it the reference to the 
 * Siebel Java Data Bean wrapper, since some things are used internally 
 * in the utility class.
 */
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility(mySiebJDB)

/*
 * These are the objects that we are going to use later. We define them
 * here in order to be able to use them in the closures we define later
 */
SiebelService  m_BusServ = null
//This is the SiebelPropertySet that is the input to the service.
SiebelPropertySet m_FieldSetIN = null
//This is the SiebelPropertySet that is the output to the service.
SiebelPropertySet m_FieldSetOUT = null
//This is the string that we are going to use to call a method on the service.
String m_sMethod = null
//Let's get a reference to the SiebelJavaDataBean.
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

/*
 * This is the getObjects() closure that get the object and component(s)
 * we want. It's defined in every script, since it is different in every
 * script. No matter how many components or objects we want to use, it is
 * better to defined them here. We also add them to the list in case we have
 * to release them in an error exit from the script.
 */
def getObjects = {
	//BUSINESS OBJECT NAME
	String busServiceName = new String("Workflow Administration")
	m_BusServ=m_dataBean.getBusService(busServiceName)
	if(m_BusServ==null)
	{
		return false;
	}
			
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusServ)
	return true
}
/*
 * This is a little closure that we will use if we exit with an error.
 */
def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}
//In this case we are going to use the Import method.
m_sMethod="Import"

/*
 * Let's call getObjects() and if it succeeds, let's continue.
 */
if(getObjects())
{
	System.out.println(myName+": SetProperties");
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	/*
	 * Here we set a couple of propeties that we are going to use later. 
	 * Here is where we map the *METHOD ARGUMENTS* to our properties. 
	 */
	Properties dProps=new Properties()
	dProps['FileName']=props['SM_XMLPath']
	dProps['FileType']="XML"
	
	/*
	 * Let's instatiate the property sets (NOT SET VALUES) that we are going to use. 
	 * The IN set is initialized with the values from dProps and the OUT
	 * is initialized empty (will be set by the Service).
	 */
	m_FieldSetIN=myUCDStepObject.createSiebelPropertySet(dProps)
	//Let's set the values for the property set. 
	myUCDStepObject.setSiebelPropertySet(m_FieldSetIN, dProps)
	//Nothing to set here, this is supposed to be empty.
	m_FieldSetOUT=myUCDStepObject.createSiebelPropertySet((Properties)null)
	if(m_FieldSetIN==null)
	{
		//Something failed with the instatiation, fail gracefully.
		exitGracefullyWithFail()
	}

	//Lets call the service.
	String lOutputPropName=new String()
	/*
	 * Let's invoke the method with the arguments we have. 
	 */
	try
	{
		m_BusServ.invokeMethod(m_sMethod, m_FieldSetIN, m_FieldSetOUT)
	}
	catch (SiebelException e)
	{
		//Something failed, exit gracefully with fail.
		exitGracefullyWithFail()
	}
	//Release the Business Service object.
	m_BusServ.release()
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
System.out.println(myName+": disconnect")
mySiebJDB.disconnect()

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");
apTool.storeOutputProperties() //write the output properties to the file