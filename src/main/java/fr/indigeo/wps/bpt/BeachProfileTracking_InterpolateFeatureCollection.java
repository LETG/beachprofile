package fr.indigeo.wps.bpt;

import org.geotools.process.factory.*;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;

import fr.indigeo.wps.bpt.tools.BeachProfileTrackingTools;

public class BeachProfileTracking_InterpolateFeatureCollection extends StaticMethodsProcessFactory<BeachProfileTracking_InterpolateFeatureCollection> {
	
	protected static BeachProfileTrackingTools callObject;

	public BeachProfileTracking_InterpolateFeatureCollection() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_InterpolateFeatureCollection.class);
		callObject = new BeachProfileTrackingTools();
	}

	@DescribeProcess(title="BeachProfileTracking_InterpolateFeatureCollection",description="Add a description of BeachProfileTracking_InterpolateFeatureCollection")
	@DescribeResult(name="result",description="the feature collection interpolated")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> BeachProfileTracking_InterpolateFeatureCollection(@DescribeParameter(name="fc",description=" the feature collection containing geometries we want to interpolate") FeatureCollection<SimpleFeatureType, SimpleFeature> fc,@DescribeParameter(name="interval",description=" distance between coordinates of the geometry") Double interval) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> result;
		result = callObject.InterpolateFeatureCollection( fc, interval);

		return result;
	}
}
