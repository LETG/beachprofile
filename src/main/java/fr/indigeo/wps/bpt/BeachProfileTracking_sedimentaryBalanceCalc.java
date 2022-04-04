package fr.indigeo.wps.bpt;

import org.geotools.process.factory.*;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;

import fr.indigeo.wps.bpt.tools.BeachProfileTrackingTools;

public class BeachProfileTracking_sedimentaryBalanceCalc extends StaticMethodsProcessFactory<BeachProfileTracking_sedimentaryBalanceCalc> {
	
	protected static BeachProfileTrackingTools callObject;

	public BeachProfileTracking_sedimentaryBalanceCalc() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_sedimentaryBalanceCalc.class);
		callObject = new BeachProfileTrackingTools();
	}

	@DescribeProcess(title="BeachProfileTracking_sedimentaryBalanceCalc",description="Add a description of BeachProfileTracking_sedimentaryBalanceCalc")
	@DescribeResult(name="result",description="a feature collection containing the results of the treatment")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> BeachProfileTracking_sedimentaryBalanceCalc(@DescribeParameter(name="profile",description=" feature collection containing multiple features of a beach profile") FeatureCollection<SimpleFeatureType, SimpleFeature> profile,@DescribeParameter(name="useSmallestDistance",description=" if useSmallestDistance is true, use the smallest distance between all features, else ignore the feature shorter than the first one") Boolean useSmallestDistance,@DescribeParameter(name="minDist",description=" specifie the minimum distance of the interval of calculation") Double minDist,@DescribeParameter(name="maxDist",description=" specifie the maximum distance of the interval of calculation") Double maxDist) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> result;
		result = callObject.sedimentaryBalanceCalc( profile, useSmallestDistance, minDist, maxDist);

		return result;
	}
}
