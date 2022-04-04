package fr.indigeo.wps.bpt.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;
import net.sf.geographiclib.GeodesicMask;

public class BeachProfileUtils {
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * Area calculation on a specific range
	 * @param coordinates Array of coordinates
	 * @param myCrs the CoordinateReferenceSytem used to format the coordinates
	 * @return Area of the profile in m²/m.l.
	 */
	public static double getProfileArea(Coordinate[] coordinates, double minDist, double maxDist, CoordinateReferenceSystem myCrs){
		if(myCrs == null) return 0;
		double area = 0;
		double totalDist = 0;
		double tempDist = 0;
		//we use GeodeticCalculator to obtain the distance between two coordinates coded with any CRS
		GeodeticCalculator gc = new GeodeticCalculator(myCrs);		
		for(int i = 0; i < coordinates.length; i++){
			if(i+1 < coordinates.length){
				try {
					//we set the start and end point of our GeodeticCalculator
					gc.setStartingPosition(JTS.toDirectPosition(coordinates[i], myCrs));
					gc.setDestinationPosition(JTS.toDirectPosition(coordinates[i+1], myCrs));
				} catch (TransformException e) {
					e.printStackTrace();
				}
				//the round value is to avoid weird result that can happen on distance calculation like 1.00000001 or 3.99999999
				tempDist = Math.round(totalDist * 1000.0) / 1000.0;
				totalDist += gc.getOrthodromicDistance(); //return the distance between starting and destination position			
				if(minDist <= tempDist && maxDist+0.01 >= Math.round(totalDist * 1000.0) / 1000.0){
					area += ((coordinates[i].z + coordinates[i+1].z)*gc.getOrthodromicDistance())/2;					
				}
			}
		}
		area = area/(maxDist-minDist);
		return area;
	}
	
	/**
	 * Get the distance between a list of coordinates
	 * @param coordinates Array of Coordinate
	 * @param myCrs the CoordinateReferenceSytem used to format the coordinates
	 * @return The total distance traveled between the points in meters
	 */
	public static double getDistanceFromCoordinates(Coordinate[] coordinates, CoordinateReferenceSystem myCrs){
		if(coordinates.length < 1 || myCrs == null) return 0;
		double totalDist = 0;
		GeodeticCalculator gc = new GeodeticCalculator(myCrs);
		for(int i = 1; i < coordinates.length; i++){
			try {
				gc.setStartingPosition(JTS.toDirectPosition(coordinates[i-1], myCrs));
				gc.setDestinationPosition(JTS.toDirectPosition(coordinates[i], myCrs));
			} catch (TransformException e) {
				e.printStackTrace();
			}
			totalDist += gc.getOrthodromicDistance();
		}
		return totalDist;
	}

	/**
	 * Create a map of LineString associated to a date from a FeatureCollection 
	 * @param featureCollection
	 * @return all LineString of the featureCollection
	 */
	public static Map<String, LineString> getProfilesFromFeature(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		Map<String, LineString> linestrings = new TreeMap<String,LineString>();
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		// get LineString from Feature
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			if (geometry instanceof LineString){
				LineString ls = (LineString) geometry;
				Collection<Property> properties = feature.getProperties();
				String dateStr = "";
				for (Property property : properties){
					try {
						dateFormat.parse(property.getValue().toString());
						dateStr = property.getValue().toString();
					} catch (ParseException e) { }
				}					
				linestrings.put(dateStr, ls);
			}
		}
		return linestrings;
	}	
	
	/**
	 * interpolate between two coordinates
	 * @param offset set an offset for the first point 
	 * @param distInterval the interval between each point
	 * @param c1
	 * @param c2
	 * @param myCrs
	 * @return
	 */
	public static LinkedList<Coordinate> InterpolateCoordinates(double offset, double distInterval, Coordinate c1, Coordinate c2, CoordinateReferenceSystem myCrs){
		if(myCrs == null) return null;
		LinkedList<Coordinate> l = new LinkedList<Coordinate>();			
		CoordinateReferenceSystem refCrs;		
		try {
			//we convert our coordinates in the WGS84 system to use the interpolation method bellow made for WGS84 only
			//this convert code could be useful for a CRS coordinates conversion tool
			refCrs = CRS.decode("EPSG:4326");
			MathTransform transform = CRS.findMathTransform(myCrs, refCrs, false);
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point1 = geometryFactory.createPoint(c1);
			Point point2 = geometryFactory.createPoint(c2);
			Point targetPoint1 = (Point) JTS.transform(point1, transform);
			Point targetPoint2 = (Point) JTS.transform(point2, transform);
			
			//interpolation algorithm
			Geodesic geod = Geodesic.WGS84;
			GeodesicData d = geod.Inverse(targetPoint1.getY(), targetPoint1.getX(), targetPoint2.getY(), targetPoint2.getX());
			GeodesicLine line = new GeodesicLine(geod, d.lat1, d.lon1, d.azi1);
			// The max number of points
			int num = (int)(d.s12 / distInterval)+1;
			//the height coefficient
			double diff = (c1.z-c2.z)/d.s12;
			l.add(new Coordinate(targetPoint1.getX(),targetPoint1.getY(),c1.z));
			for (int i = 1; i <= num; i++) {
				if((i * distInterval)-offset < d.s12){
					GeodesicData g = line.Position((i * distInterval)-offset, GeodesicMask.LATITUDE | GeodesicMask.LONGITUDE);
					l.add(new Coordinate(g.lon2,g.lat2,c1.z - diff*((distInterval*i)-offset)));
				}
			}
			l.add(new Coordinate(targetPoint2.getX(),targetPoint2.getY(),c2.z));		
		} catch (FactoryException e1) {
			e1.printStackTrace();
		} catch (MismatchedDimensionException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return l;
	}
}
