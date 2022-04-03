package remoteWpsCall;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.xml.sax.SAXException;

public class WMSrequest {

		private String ServerName ;
		private String layer;
		private int versionNumber;
		private String ImageType ;
		private int Width;
		private int Height ;
		private boolean isTransparent ;
		private String SRS;
		private double minx,miny,maxx,maxy;
		
	public WMSrequest(String urlName, String aLayer, int aVersionNumber, String atypeImage, int aWidth,  int aHeight,boolean transparence,String aSRS, 
			double minx,double miny,double maxx, double maxy)
			throws ServiceException, IOException {
				this.ServerName = urlName;
				this.layer = aLayer;
				this.versionNumber = 0;
				if (aVersionNumber == 1) {
					this.versionNumber = aVersionNumber;
				} else {
					this.versionNumber = 0;
				}
				this.ImageType = atypeImage;
				this.Width=aWidth;
				this.Height=aHeight;
				this.isTransparent = transparence;
				this.SRS=aSRS;
				this.minx=minx;
				this.miny=miny;
				this.maxx=maxx;
				this.maxy=maxy;	
		}
		
	public BufferedImage Call( ) throws ServiceException, IOException{
			
			URL url = null;
			try {
			  url = new URL("https://"+ServerName+"?VERSION=1.1.1&Request=GetCapabilities&Service=WMS");
			} catch (MalformedURLException e) {
			}
			
			WebMapServer wms = null;
			try {
			  wms = new WebMapServer(url);
			} catch (IOException e) {				
			  //There was an error communicating with the server
			  //For example, the server is down			
			} catch (ServiceException e) {			
			  //The server returned a ServiceException (unusual in this case)
			} catch (SAXException e) {		
			  //Unable to parse the response from the server
			  //For example, the capabilities it returned was not valid		
			}
			
			WMSCapabilities capabilities = wms.getCapabilities();

			org.geotools.data.wms.request.GetMapRequest request = wms.createGetMapRequest();
			for ( org.geotools.data.ows.Layer layer : WMSUtils.getNamedLayers(capabilities) ) {
				if (layer.getTitle().equals(this.layer)) {
					request.addLayer(layer);
				}
			}
			request.setFormat("image/"+this.ImageType);
			//sets the dimensions of the image to be returned from the server
			request.setDimensions(this.Width, this.Height); 
			request.setTransparent(this.isTransparent);
			request.setSRS(SRS);
			
			String bbox;
			// version 1.3.0 and EPSG:4326
			if (this.versionNumber == 1 && this.SRS.equals("EPSG:4326")) {
				bbox = String.valueOf(miny)+","+String.valueOf(minx)+","+String.valueOf(maxy)+","+String.valueOf(maxx);
			} else {
				bbox = String.valueOf(minx)+","+String.valueOf(miny)+","+String.valueOf(maxx)+","+String.valueOf(maxy);
			}
			request.setBBox(bbox);
			
			for ( org.geotools.data.ows.Layer layer : WMSUtils.getNamedLayers(capabilities) ) {
				  request.addLayer(layer);
			}
			
			GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
			BufferedImage image = ImageIO.read(response.getInputStream());
			return image;
	}
}
