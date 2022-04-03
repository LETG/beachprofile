package remoteWpsCall;

import java.io.IOException;
import java.net.*;
import java.util.*;

import net.opengis.wps10.*;
import net.opengis.wps10.impl.*;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geotools.data.wps.*;
import org.geotools.data.wps.request.*;
import org.geotools.data.wps.response.*;
import org.geotools.ows.ServiceException;

import org.locationtech.jts.io.*;
import org.locationtech.jts.geom.*;

public class RemoteWpsCall {
	
	private String ServerName ;
	private WebProcessingService wps;
	private String theWpsId;
	private URL url;
	
	public RemoteWpsCall(String aServerName, String aWpsId) {
		this.ServerName = aServerName;
		this.theWpsId = aWpsId;
		try {
			url = new URL(ServerName+"?service=WPS&request=GetCapabilities");
				try {
					wps = new WebProcessingService(url);
				} catch (ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public String ToString() {
			String result = "";
			
			WPSCapabilitiesType capabilities = wps.getCapabilities();
			ProcessOfferingsType processOfferings = capabilities.getProcessOfferings();
			EList<ProcessBriefTypeImpl> processes = processOfferings.getProcess();
			int i=0;
			for (ProcessBriefTypeImpl process : processes) {
				result+=process.getIdentifier().getValue()+" "+i+"\n";
				i++;
			}
			
			return result;
	}
	
	public String Request(ArrayList<Object> theWpsInputs) {

		// creation requete de description
		DescribeProcessRequest descRequest = wps.createDescribeProcessRequest();
		descRequest.setIdentifier(theWpsId); 
		
		try {
			// creation type de requete et type d'entrées
			DescribeProcessResponse descResponse = wps.issueRequest(descRequest);
			ProcessDescriptionsType T_processDesc = descResponse.getProcessDesc();
			ProcessDescriptionType T_Processdescription = (ProcessDescriptionType) T_processDesc.getProcessDescription().get(0);
			
			InputDescriptionType[] T_InputDescription = new InputDescriptionType[theWpsInputs.size()];
			for (int i = 0; i < T_InputDescription.length; i++) {
				T_InputDescription[i] = (InputDescriptionType) T_Processdescription.getDataInputs().getInput().get(i); 
			} 	        	      
	        
	        // création des requetes d'execution			
		    ExecuteProcessRequest exeRequest = wps.createExecuteProcessRequest();
		    exeRequest.setIdentifier(theWpsId);
		    
	        // création des entrées pour les requetes d'execution			    
            DataType[] T_input = new DataType[theWpsInputs.size()];
          
            List<List<EObject>> listT_Input = new ArrayList<List<EObject>>();
            for (int i = 0; i < T_InputDescription.length; i++) {
            	if (! (theWpsInputs.get(i) instanceof Geometry)) {
            		T_input[i] = WPSUtils.createInputDataType(theWpsInputs.get(i), T_InputDescription[i]);
            	} else {
            		T_input[i] = WPSUtils.createInputDataType(new CDATAEncoder(((Geometry) theWpsInputs.get(i)).toText()),WPSUtils.INPUTTYPE_COMPLEXDATA, null,"application/wkt");
            	}
            	listT_Input.add(new ArrayList<EObject>());
            	listT_Input.get(i).add(T_input[i]);           
            	exeRequest.addInput(T_InputDescription[i].getIdentifier().getValue(), listT_Input.get(i)); 
 			}            		 
           
            // execution du WPS
            ExecuteProcessResponse responseHW = wps.issueRequest(exeRequest); 
            
            // recuperation des sorties
            ExecuteResponseType executeResponseHW = responseHW.getExecuteResponse();  
//          ExceptionReportType exceptionResponseHW = responseHW.getExceptionResponse(); 
            EList<OutputDataType> outputsHW = executeResponseHW.getProcessOutputs().getOutput(); 
            OutputDataType outputHW = outputsHW.get(0); 
            String resultHW = (String) outputHW.getData().getLiteralData().getValue();                     
    		return resultHW; 
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return "KO"; 
	}
}
