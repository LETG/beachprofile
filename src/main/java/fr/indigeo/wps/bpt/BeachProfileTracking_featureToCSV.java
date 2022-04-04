package fr.indigeo.wps.bpt;

import org.geotools.process.factory.*;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;

import fr.indigeo.wps.bpt.tools.BeachProfileTrackingTools;

public class BeachProfileTracking_featureToCSV extends StaticMethodsProcessFactory<BeachProfileTracking_featureToCSV> {
	
	protected static BeachProfileTrackingTools callObject;

	public BeachProfileTracking_featureToCSV() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_featureToCSV.class);
		callObject = new BeachProfileTrackingTools();
	}

	@DescribeProcess(title="BeachProfileTracking_featureToCSV",description="Add a description of BeachProfileTracking_featureToCSV")
	@DescribeResult(name="result",description="A string containing feature collection informations formatted to csv format")
	public static String BeachProfileTracking_featureToCSV(@DescribeParameter(name="featureCollection",description=" the featureCollection we want to display in csv") FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		String result;
		result = callObject.featureToCSV( featureCollection);

		return result;
	}
}
