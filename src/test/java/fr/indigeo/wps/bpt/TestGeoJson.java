package fr.indigeo.wps.bpt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import fr.indigeo.wps.bpt.tools.BeachProfileTrackingTools;
import fr.indigeo.wps.bpt.tools.FeatureCollectionValidation;
import fr.indigeo.wps.bpt.tools.GeoJsonUtils;

/**
 * Class used for local testing of methods instead of build a wps 
 * @author Quentin Lechat
 *
 */
public class TestGeoJson {

	private static final Logger LOGGER = Logger.getLogger(TestGeoJson.class);

	/**
	 * 
	 * @param dataString
	 * @param dataDir
	 * @param fileName
	 */
	public static void createFile(String dataString, File dataDir, String fileName) {
		BufferedWriter bw = null;
		try {

			bw = new BufferedWriter(new FileWriter(new File(dataDir, fileName)));
			bw.write(dataString); // Replace with the string you are trying to write
		} catch (IOException e) {
			LOGGER.error("erreur entrées sorties", e);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				LOGGER.error("erreur entrées sorties", e);
			}
		}

	}

	@Test
	public void testGeoJson () {
		long startTime = System.nanoTime();

		File dataDir = new File("data");
		File beachProfileFile = new File(dataDir, "prf1_vougot.json");
		
		BeachProfileTrackingTools bp = new BeachProfileTrackingTools();
		
		FeatureCollectionValidation fcv = new FeatureCollectionValidation();
		try {
			createFile(bp.featureToCSV(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0, true, 0, 0)), dataDir, "result0.csv");
			createFile(bp.featureToCSV(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.5, true, 0, 0)), dataDir, "result05.csv");
			createFile(bp.featureToCSV(bp.InterpolateFeatureCollection(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0)), dataDir, "resultInterpol0.csv");
			createFile(bp.featureToCSV(bp.InterpolateFeatureCollection(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.5)), dataDir, "resultInterpol05.csv");
			//createFile(bp.featureToJSON(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.1, true, 0, 0)), dataDir, "result.json");

		} catch (IOException e) {
			LOGGER.error("Erreur lors de la crétaion du fichier");
		}

		//get the execution time
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/1000000 ;
		LOGGER.info("Temps de création : " + duration + "ms");
		
	}
}
