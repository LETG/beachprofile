package fr.indigeo.wps.bpt.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

public class BeachProfileTrackingTools {

	private static final Logger LOGGER = LogManager.getLogger(BeachProfileTrackingTools.class);

	public BeachProfileTrackingTools() {}

	public LineString interpolateLineStringConstantStep(LineString line, double step, CoordinateReferenceSystem crs) {

		GeometryFactory gf = new GeometryFactory();
		Coordinate[] coords = line.getCoordinates();

		List<Coordinate> result = new ArrayList<>();
		result.add(coords[0]); // on part de la tête du profil

		GeodeticCalculator gc = new GeodeticCalculator(crs);

		double cumulative = 0.0;
		double target = step;

		for (int i = 1; i < coords.length; i++) {

			// Distance du segment i-1 → i
			try {
				gc.setStartingPosition(JTS.toDirectPosition(coords[i - 1], crs));
				gc.setDestinationPosition(JTS.toDirectPosition(coords[i], crs));
			} catch (TransformException e) {
				
				LOGGER.error("Erreur durant l'interpolation",e);
				continue; 
			}
			double segmentLength = gc.getOrthodromicDistance();

			// Tant que le point recherché cible se trouve dans ce segment :
			while (cumulative + segmentLength >= target) {
				double ratio = (target - cumulative) / segmentLength;
				double x = coords[i - 1].x + ratio * (coords[i].x - coords[i - 1].x);
				double y = coords[i - 1].y + ratio * (coords[i].y - coords[i - 1].y);

				result.add(new Coordinate(x, y));

				target += step; 
			}

			cumulative += segmentLength;
		}

		return gf.createLineString(result.toArray(new Coordinate[0]));
	}
	
	/**
	 * Do an interpolation for each Feature's Geometry of a FeatureCollection with an interval
	 * @param fc
	 * @param interval in meters
	 * @return
	 */
	public FeatureCollection<SimpleFeatureType, SimpleFeature> InterpolateFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> fc, double interval){
		if(interval <= 0){
			return fc;
		}
		// load the LineStrings
		// With geoserver 2.21.5 version CRS give WGS84 instead of 2154
		CoordinateReferenceSystem myCrs;
		try {
			myCrs = CRS.decode("EPSG:2154"); // fc.getSchema().getCoordinateReferenceSystem();
		
			GeometryFactory geometryFactory = new GeometryFactory();
			DefaultFeatureCollection resultFeatureCollection = null;
			// get Linestrings order by date
			Map<Date, LineString> lineStrings = BeachProfileUtils.getProfilesFromFeature(fc);
			Map<Date, LineString> interpolatedLineStrings = new HashMap<Date,LineString>();
			
			// do the interpolation
			lineStrings.forEach((id,line) -> {
				LineString interpolated = interpolateLineStringConstantStep(line, interval, myCrs);
				interpolatedLineStrings.put(id, interpolated);
			});
			
			//create a new FeatureCollection to add the new coordinates
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setCRS(myCrs);
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("date", String.class);

			
			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			// add geometrie to defaultFeatures
			for (Entry<Date, LineString> entry : interpolatedLineStrings.entrySet())
			{
				simpleFeatureBuilder.add(entry.getValue());
				simpleFeatureBuilder.add(entry.getKey());
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(entry.getKey() + ""));
			}
			
			return resultFeatureCollection;
		} 
		catch (FactoryException e) {
			LOGGER.debug("FactoryException",e);
						
			return null;
		}
	}
	
	/**
	 * Calculate the area of sediments for a length and compare it between each Feature
	 * @param profile
	 * @param useSmallestDistance
	 * @param minDist
	 * @param maxDist
	 * @return
	 */
	public FeatureCollection<SimpleFeatureType, SimpleFeature> sedimentaryBalanceCalc(FeatureCollection<SimpleFeatureType, SimpleFeature> profile, boolean useSmallestDistance, double minDist, double maxDist) {
		Coordinate[] coordinates = null;
		
		// load the LineStrings
		// With geoserver 2.21.5 version CRS give WGS84 instead of 2154
		CoordinateReferenceSystem myCrs;
		try {
			myCrs = CRS.decode("EPSG:2154"); // fc.getSchema().getCoordinateReferenceSystem();
		
			//create a new FeatureCollection to write the calculation results
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName("featureType");
			b.add("date", String.class);
			b.add("volume", Double.class);
			b.add("diffWithPrevious", Double.class);
			b.add("totalEvolution", Double.class);
			SimpleFeatureType type = b.buildFeatureType();
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);		
			DefaultFeatureCollection dfc = new DefaultFeatureCollection();
			
			Map<Date, LineString> refProfile = BeachProfileUtils.getProfilesFromFeature(profile);
			double refProfileArea = 0;
			double lastProfileArea = 0;
			double tempProfileArea = 0;
			double tempProfileDist = 0;
			double totalEvolution = 0;
			double tempMaxDist = 0;

			// For each profile 
			for (Entry<Date, LineString> entry : refProfile.entrySet()) {
				coordinates = entry.getValue().getCoordinates();
			
				LOGGER.debug("Calulation for date {}", entry.getKey().toString());
				if(refProfileArea == 0){
					//if we don't specify maxDist, check ignoreDateWithLessDist					
					//if useSmallestDistance is false, ignore the feature with a distance less than the distance of the first date
					//else if useSmallestDistance is true, use the smallest distance of all features
					tempMaxDist = BeachProfileUtils.getDistanceFromCoordinates(coordinates, myCrs);
					if(useSmallestDistance){
						LOGGER.debug("Use smallestDistance {}", useSmallestDistance);
						// vérification par rapport aux autres profils
						for (Entry<Date, LineString> entry2 : refProfile.entrySet()) {
							double dist = BeachProfileUtils.getDistanceFromCoordinates(entry2.getValue().getCoordinates(), myCrs);
							tempMaxDist = dist < tempMaxDist ? dist : tempMaxDist;						
						}
					}
					//handle min/max issues
					if(maxDist > tempMaxDist || maxDist <= 0) maxDist = tempMaxDist;
					if(minDist < 0) minDist = 0;
					if(minDist >= maxDist) minDist = 0;					
				
					refProfileArea = lastProfileArea = BeachProfileUtils.getProfileArea(coordinates, minDist, maxDist, myCrs);
					//write the result. For the first date we don't have evolutions values so we add a 0 value
					builder.add(entry.getKey().toString());
					builder.add(lastProfileArea);
					builder.add(0);
					builder.add(0);
					SimpleFeature sf = builder.buildFeature(null);
					dfc.add(sf);
				}
				else{
					tempProfileDist = BeachProfileUtils.getDistanceFromCoordinates(coordinates, myCrs);
					if(tempProfileDist < maxDist){
						LOGGER.debug(entry.getKey().toString() + " | " + tempProfileDist + " | distance at this date is less than the distance wanted");
					}
					else{
						tempProfileArea = BeachProfileUtils.getProfileArea(coordinates, minDist, maxDist, myCrs);
						totalEvolution += (tempProfileArea - lastProfileArea);
						//write the results
						builder.add(entry.getKey().toString());
						builder.add(tempProfileArea);
						builder.add((tempProfileArea - lastProfileArea));
						builder.add(totalEvolution);
						SimpleFeature sf = builder.buildFeature(null);
						dfc.add(sf);
						lastProfileArea = tempProfileArea;
					}		
				}
			}
			
			return dfc;
		} catch (FactoryException e1) {
			LOGGER.error("FactoryException",e1);	
			return null;
		}
	}

	/**
	 * Convert the FeatureColleciton to a string which is then readable in .csv format
	 * @param featureCollection
	 * @return
	 */
	public String featureToCSV(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		String csvString = "";
		
		//get column name from the features properties
		List<AttributeType> attributes = featureCollection.getSchema().getTypes();
		for(AttributeType att : attributes) csvString += att.getName() + ";";
		csvString +="\n";
		
		//loop in the featureCollection, create a new line for each feature and add recovered data
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();			
			for(int i = 0; i< feature.getAttributeCount(); i++)	csvString += feature.getAttribute(i) + ";";
			csvString += "\n";
		}
		
		return csvString;
	}
	
	/**
	 * Convert the FeatureColleciton to a json string object
	 * @param featureCollection
	 * @return
	 */
	public String featureToJSON(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		
		JSONObject result = new JSONObject();
		JSONArray resultsArray = new JSONArray();
		final String dateKey = "date";
		
		//loop in the featureCollection, create a new line for each feature and add recovered data
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		while (iterator.hasNext()) {
			JSONObject bpf = new JSONObject();
			JSONArray bpfValues = new JSONArray();
			SimpleFeature feature = iterator.next();

			if(LOGGER.isDebugEnabled()){
				Collection<Property> properties = feature.getProperties();
				for (Property property : properties){
					LOGGER.debug("key : {},  value : {} " , property.getName(), property.getValue().toString());
				}
			}

			if(feature.getProperty("error") != null){
				result.put("result", "error");
				result.put("additional", feature.getProperty("error").getValue());
				return result.toString();
			}
			bpf.put("date", feature.getProperty(dateKey).getValue().toString());

			for (Property property : feature.getProperties()) {
				if(!dateKey.equals(property.getName().toString())){
					JSONObject bpfValue = new JSONObject();
					bpfValue.put(property.getName().toString(), property.getValue());	
					bpfValues.add(bpfValue);
				}
			}		
			bpf.put("data", bpfValues);
			resultsArray.add(bpf);
		}
		iterator.close();

		result.put("result", resultsArray);
		LOGGER.debug(result.toString());
		return result.toString();
	}

}
