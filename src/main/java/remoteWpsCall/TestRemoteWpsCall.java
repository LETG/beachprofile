package remoteWpsCall;

import java.util.ArrayList;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.geom.*;

public class TestRemoteWpsCall {

	public static void main(String[] args) {


		try {
			Integer bbb;
			ArrayList<Object> inputs= new ArrayList<Object>();
			Integer aaa1Cp = 1 ;
			inputs.add(aaa1Cp);
			Integer aaa2Cp = 2 ;
			inputs.add(aaa2Cp);
			
			RemoteWpsCall aCall = new RemoteWpsCall("http://localhost:8080/geoserver/ows","TestWPS:addInt");
			String result = aCall.Request(inputs);
			
            bbb = Integer.parseInt(result);
			System.out.println(bbb);
			
			ArrayList<Object> inputs2= new ArrayList<Object>();

			GeometryFactory factory = new GeometryFactory();
			Coordinate coordinate = new Coordinate(7.0, 6.0);
			Point point = factory.createPoint(coordinate);
			inputs2.add(point);	
			
			RemoteWpsCall aCall2 = new RemoteWpsCall("http://localhost:8080/geoserver/ows","JTS:getX");
			String resultS = aCall2.Request(inputs2);
			
            double ddd = Double.parseDouble(resultS);
			System.out.println(ddd);
			
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
