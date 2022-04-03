package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeoJsonUtils {
	private static FeatureJSON featureJSON;
	
	/**
	 * Write a file to a string
	 * @param jsonFile
	 * @return String containing file content 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	private static String geoJsonToString(File jsonFile) throws FileNotFoundException, IOException, ParseException {	
		JSONParser jsonParser = new JSONParser();
		String data = jsonParser.parse(new FileReader(jsonFile)).toString();	
		return data;
	}
	
	/**
	 * Check if a file contains a FeatureCollection
	 * @param jsonFile
	 * @return if a feature has been found or not
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static boolean isFeatureCollectionData(File jsonFile) throws FileNotFoundException, IOException {	
		try {
			String data = geoJsonToString(jsonFile);
			JSONObject jsonData = new JSONObject(data);
			return jsonData.get("type").equals("FeatureCollection");
		} catch (ParseException e) {
			return false;
		} catch (JSONException e) {
			return false;
		}	
	}
	
	/**
	 * check if the geometries in a featureCollection are of a type given 
	 * @param featureCollection
	 * @param type
	 * @return
	 */
	public static boolean isGeometryType(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, String type) {
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		boolean res = true;
		while (iterator.hasNext() && res) {
			SimpleFeature feature = iterator.next();
			if(!feature.getDefaultGeometry().getClass().getSimpleName().equals(type)) res = false;
		}
		return res;
	}

	/**
	 * Find the CoordinateReferenceSystem in a file
	 * @param f the file we search in
	 * @return CoordinateReferenceSystem
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CoordinateReferenceSystem geoJsonToCoordinateReferenceSystem(File f) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON();
		return featureJSON.readCRS(f);
	}	

	/**
	 * create a feature collection from a file
	 * @param featureCollectionFile the file containing a feature collection
	 * @return a feature collection
	 * @throws FileNotFoundException
	 * @throws IOException
	 */	@SuppressWarnings("unchecked")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> geoJsonToFeatureCollection(File featureCollectionFile) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON();
		return featureJSON.readFeatureCollection(new FileInputStream(featureCollectionFile));
	}

	/**
	 * Create a GeoJson from a FeatureCollection
	 * @param featureCollection the FeatureCollection we want to write in a file
	 * @param dir the full path to the write directory
	 * @param fileName the name of the file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void featureCollectionToGeoJsonFile(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, File dir, String fileName) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON(new GeometryJSON(15));
		featureJSON.writeFeatureCollection(featureCollection, new FileOutputStream(new File(dir, fileName + ".json")));
	}
}