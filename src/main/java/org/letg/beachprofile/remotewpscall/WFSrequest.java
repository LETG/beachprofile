package org.letg.beachprofile.remotewpscall;;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.geotools.data.simple.*;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.xml.XMLSAXHandler;

public class WFSrequest {

		private String serverName;
		private String layer;
		
	    private WFSDataStore wfs;
		
	public WFSrequest(String urlName, String aLayer) {
				this.serverName = urlName;
				this.layer = aLayer;
		}
		
	public FeatureCollection<SimpleFeatureType, SimpleFeature> Call( ) {
			
			FeatureCollection<SimpleFeatureType, SimpleFeature> retour = null;
		
			URL url = null;
			try {
				url = new URL("http://"+serverName);
				XMLSAXHandler.setLogLevel(Level.OFF); 
		        Map connectionParameters  = new HashMap();
		        connectionParameters.put(WFSDataStoreFactory.URL.key, url);
		        WFSDataStoreFactory  dsf = new WFSDataStoreFactory();
		        wfs = dsf.createDataStore(connectionParameters);
		        SimpleFeatureType schema = wfs.getSchema( layer );
		        SimpleFeatureSource source = wfs.getFeatureSource( layer );
		        retour = source.getFeatures();
		      } catch (MalformedURLException e) {
		        e.printStackTrace();
		      } catch (IOException e) {
		        e.printStackTrace();
		     }

			return retour;
	}
}
