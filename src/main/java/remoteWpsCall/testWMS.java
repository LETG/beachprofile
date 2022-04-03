package remoteWpsCall;

import java.awt.image.BufferedImage;

public class testWMS {
	
	public static void main(String[] args) {

	WMSrequest aWMSrequest ;
	BufferedImage image = null;
	
	try {
		aWMSrequest = new WMSrequest("geobretagne.fr/geoserver/paysstbrieuc/ows","ZONE_URBA_rnu",0,"png",200,200,false,"CRS:84",-3.0,48.37,-2.97,48.4);
		image = aWMSrequest.Call();			
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	System.out.println(image.getNumXTiles()+" "+image.getNumYTiles());
	}
}
