package remoteWpsCall;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class testWFSCall {
	
	public static void main(String[] args) {

	WFSrequest testWfsCall = new WFSrequest("portail.indigeo.fr/geoserver/LETG-BREST/ows", "LETG-BREST:TDC_VOUG");
// http://portail.indigeo.fr/geoserver/LETG-BREST/ows?service=WFS&request=GetFeature&typeName=LETG-BREST:REF_VOUG&outputFormat=json
	
	FeatureCollection<SimpleFeatureType, SimpleFeature> retour = null;

	retour = testWfsCall.Call();
	System.out.println(retour.size()); 
	
	FeatureJSON featureJSON = new FeatureJSON();
	try {
		featureJSON.writeFeatureCollection(retour, new FileOutputStream(new File("./data/", "test" + ".json")));
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
}
