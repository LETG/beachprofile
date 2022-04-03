package org.letg.beachprofile.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

public class BeachProfileTrackingTools {

	public BeachProfileTrackingTools() {}
	
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
		//load the LineStrings
		CoordinateReferenceSystem myCrs = fc.getSchema().getCoordinateReferenceSystem();
		GeometryFactory geometryFactory = new GeometryFactory();
		DefaultFeatureCollection resultFeatureCollection = null;
		Map<String, LineString> lineStrings = BeachProfileUtils.getProfilesFromFeature(fc);
		Map<String, LineString> interpolatedLineStrings = new HashMap<String,LineString>();
		
		//do the interpolation
		lineStrings.forEach((a,b) -> {
			if(b.getNumPoints() > 1){
				Coordinate[] coordinates = b.getCoordinates();
				LinkedList<Coordinate> newCoordinates = new LinkedList<Coordinate>();				
				LinkedList<Coordinate> tempList;
				double offset = 0.0;
				double totalDist = 0.0;
				//iterate through each point to create a number of new point between.
				//the offset is used to stack the distance remained at the end of each interpolation. It is then add to the next interpolation
				//With the offset we are sure to have a new point at the same interval plus the original points between them.
				for (int i = 1; i < coordinates.length; i++) {
					GeodeticCalculator gc = new GeodeticCalculator(myCrs);
						try {
							gc.setStartingPosition(JTS.toDirectPosition(coordinates[i-1], myCrs));
							gc.setDestinationPosition(JTS.toDirectPosition(coordinates[i], myCrs));
						} catch (TransformException e) {
							e.printStackTrace();
						}
					double dist = gc.getOrthodromicDistance();
					totalDist += dist;
					tempList = BeachProfileUtils.InterpolateCoordinates(offset, interval, coordinates[i-1], coordinates[i], myCrs);
					if(i != coordinates.length -1) tempList.removeLast();
					newCoordinates.addAll(tempList);
					offset = totalDist%interval;
			    }				
				//create new linestring containing all the coordinates
				LineString ls = geometryFactory.createLineString(newCoordinates.toArray(new Coordinate[newCoordinates.size()]));
				interpolatedLineStrings.put(a, ls);
			}
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
		for (Entry<String, LineString> entry : interpolatedLineStrings.entrySet())
		{
			simpleFeatureBuilder.add(entry.getValue());
			simpleFeatureBuilder.add(entry.getKey());
			resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(entry.getKey() + ""));
		}
		return resultFeatureCollection;
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
		CoordinateReferenceSystem myCrs = profile.getSchema().getCoordinateReferenceSystem();
		//create a new FeatureCollection to write the calculation results
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("featureType");
		b.add("date", String.class);
		b.add("volume", Double.class);
		b.add("diffWithPrevious", Double.class);
		b.add("previousEvolutionPercent", Double.class);
		b.add("totalEvolutionPercent", Double.class);
		SimpleFeatureType type = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);		
		DefaultFeatureCollection dfc = new DefaultFeatureCollection();
		
		Map<String, LineString> refProfile = BeachProfileUtils.getProfilesFromFeature(profile);
		double refProfileArea = 0;
		double lastProfileArea = 0;
		double tempProfileArea = 0;
		double tempProfileDist = 0;
		double totalEvolutionPercent = 0;
		double tempMaxDist = 0;
		for (Entry<String, LineString> entry : refProfile.entrySet()) {
			coordinates = entry.getValue().getCoordinates();
			if(refProfileArea == 0){
				//if we don't specify maxDist, check ignoreDateWithLessDist					
				//if useSmallestDistance is false, ignore the feature with a distance less than the distance of the first date
				//else if useSmallestDistance is true, use the smallest distance of all features
				tempMaxDist = BeachProfileUtils.getDistanceFromCoordinates(coordinates, myCrs);
				if(useSmallestDistance){
					for (Entry<String, LineString> entry2 : refProfile.entrySet()) {
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
				builder.add(BeachProfileUtils.getProfileArea(coordinates, minDist, maxDist, myCrs));
				builder.add(0);
				builder.add(0);
				builder.add(0);
				SimpleFeature sf = builder.buildFeature(null);
				dfc.add(sf);
			}
			else{
				tempProfileDist = BeachProfileUtils.getDistanceFromCoordinates(coordinates, myCrs);
				if(tempProfileDist < maxDist){
					System.out.println(entry.getKey().toString() + " | " + tempProfileDist + " | distance at this date is less than the distance wanted");
				}
				else{
					tempProfileArea = BeachProfileUtils.getProfileArea(coordinates, minDist, maxDist, myCrs);
					totalEvolutionPercent += (tempProfileArea - lastProfileArea)/lastProfileArea*100;
					//write the results
					builder.add(entry.getKey().toString());
					builder.add(tempProfileArea);
					builder.add((tempProfileArea - lastProfileArea));
					builder.add((tempProfileArea - lastProfileArea)/lastProfileArea*100);
					builder.add(totalEvolutionPercent);
					SimpleFeature sf = builder.buildFeature(null);
					dfc.add(sf);
					lastProfileArea = tempProfileArea;
				}		
			}
        }
		return dfc;
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
	 * Create a csv file from a featurecollection, use for local testing 
	 * @param featureCollection
	 * @param dataDir
	 * @param fileName
	 * @return
	 */
	public boolean createCSVFile(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, File dataDir, String fileName) {
		
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
		
		//create file
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(new File(dataDir, fileName)));
			bw.write(csvString);
		} catch (IOException e) {
			System.out.println("erreur entrées sorties");
			return false;
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				System.out.println("erreur entrées sorties");
				return false;
			}
		}
		return true;
	}

}
