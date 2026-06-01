package fr.indigeo.wps.bpt.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class BeachProfileTrackingToolsTest {

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final BeachProfileTrackingTools tools = new BeachProfileTrackingTools();

    @Test
    void reprojectFeatureCollectionToRefLine_returnsOriginalWhenRefLineNull() {
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = new DefaultFeatureCollection();

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, null);

        assertSame(fc, result);
    }

    @Test
    void reprojectFeatureCollectionToRefLine_projectsEachVertexToReference() {
        SimpleFeatureType type = buildLineType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection(null, type);
        DefaultFeatureCollection refLine = new DefaultFeatureCollection(null, type);

        LineString profile = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0)
        });
        fc.add(buildFeature(type, "profile-1", profile));

        LineString reference = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 5)
        });
        refLine.add(buildFeature(type, "refline-1", reference));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, refLine);
        SimpleFeature reprojected = result.features().next();
        LineString shifted = (LineString) reprojected.getDefaultGeometry();

        Coordinate[] expected = new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 0),
            new Coordinate(10, 0)
        };
        assertArrayEquals(expected, shifted.getCoordinates());
    }

    @Test
    void reprojectFeatureCollectionToRefLine_usesDefaultDistanceMaxWhenNotProvided() {
        SimpleFeatureType type = buildLineType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection(null, type);
        DefaultFeatureCollection refLine = new DefaultFeatureCollection(null, type);

        LineString profile = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(50, 0),
            new Coordinate(60, 0)
        });
        fc.add(buildFeature(type, "profile-1", profile));

        LineString reference = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(0, 10)
        });
        refLine.add(buildFeature(type, "refline-1", reference));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, refLine);
        SimpleFeature reprojected = result.features().next();
        LineString shifted = (LineString) reprojected.getDefaultGeometry();

        Coordinate[] expected = new Coordinate[0];
        assertArrayEquals(expected, shifted.getCoordinates());
    }

    @Test
    void reprojectFeatureCollectionToRefLine_skipsVerticesFurtherThanDistanceMax() {
        SimpleFeatureType type = buildLineType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection(null, type);
        DefaultFeatureCollection refLine = new DefaultFeatureCollection(null, type);

        LineString profile = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(20, 0)
        });
        fc.add(buildFeature(type, "profile-1", profile));

        LineString reference = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 5)
        });
        refLine.add(buildFeature(type, "refline-1", reference));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, refLine, 5d);
        SimpleFeature reprojected = result.features().next();
        LineString shifted = (LineString) reprojected.getDefaultGeometry();

        Coordinate[] expected = new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 0)
        };
        assertArrayEquals(expected, shifted.getCoordinates());
    }

    @Test
    void interpolateFeatureCollection_keepsReferenceLineDistances() throws Exception {
        SimpleFeatureType type = buildAnalysisType();
        DefaultFeatureCollection profiles = new DefaultFeatureCollection(null, type);
        DefaultFeatureCollection refLine = new DefaultFeatureCollection(null, buildLineType());

        profiles.add(buildAnalysisFeature(type, "profile-1", date("2024-01-01"),
            geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(140020, 6858654, 20),
                new Coordinate(140050, 6858654, 50)
            }),
            0d,
            0d));
        refLine.add(buildFeature(buildLineType(), "refline-1",
            geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(140000, 6858654),
                new Coordinate(140100, 6858654)
            })));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.InterpolateFeatureCollection(profiles, refLine, 10d);

        try (FeatureIterator<SimpleFeature> iterator = result.features()) {
            SimpleFeature interpolated = iterator.next();
            LineString line = (LineString) interpolated.getDefaultGeometry();

            assertEquals(20d, (Double) interpolated.getAttribute("startdistance"), 1d);
            assertEquals(50d, (Double) interpolated.getAttribute("enddistance"), 1d);
            assertEquals(140020d, line.getCoordinateN(0).x, 1d);
        }
    }

    @Test
    void sedimentaryBalanceCalc_usesAbsoluteReferenceDistancesForComparableArea() throws Exception {
        SimpleFeatureType type = buildAnalysisType();
        DefaultFeatureCollection profiles = new DefaultFeatureCollection(null, type);

        profiles.add(buildAnalysisFeature(type, "profile-1", date("2024-01-01"),
            geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(140010, 6858654, 10),
                new Coordinate(140020, 6858654, 20),
                new Coordinate(140030, 6858654, 30),
                new Coordinate(140040, 6858654, 40),
                new Coordinate(140050, 6858654, 50)
            }),
            10d,
            50d));

        profiles.add(buildAnalysisFeature(type, "profile-2", date("2024-02-01"),
            geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(140000, 6858654, 0),
                new Coordinate(140010, 6858654, 10),
                new Coordinate(140020, 6858654, 20),
                new Coordinate(140030, 6858654, 30),
                new Coordinate(140040, 6858654, 40),
                new Coordinate(140050, 6858654, 50)
            }),
            0d,
            50d));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.sedimentaryBalanceCalc(profiles, true, 0d, 0d);

        try (FeatureIterator<SimpleFeature> iterator = result.features()) {
            SimpleFeature first = iterator.next();
            SimpleFeature second = iterator.next();

            assertEquals((Double) first.getAttribute("volume"), (Double) second.getAttribute("volume"), 1d);
            assertEquals(0d, (Double) second.getAttribute("diffWithPrevious"), 1d);
        }
    }

    private SimpleFeatureType buildLineType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("lineType");
        builder.add("geometry", LineString.class);
        return builder.buildFeatureType();
    }

    private SimpleFeatureType buildAnalysisType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("analysisType");
        builder.add("geometry", LineString.class);
        builder.add("date", Date.class);
        builder.add("startdistance", Double.class);
        builder.add("enddistance", Double.class);
        return builder.buildFeatureType();
    }

    private SimpleFeature buildFeature(SimpleFeatureType type, String id, LineString geometry) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.add(geometry);
        return builder.buildFeature(id);
    }

    private SimpleFeature buildAnalysisFeature(SimpleFeatureType type, String id, Date date, LineString geometry, double startDistance, double endDistance) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.add(geometry);
        builder.add(date);
        builder.add(startDistance);
        builder.add(endDistance);
        return builder.buildFeature(id);
    }

    private Date date(String value) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(value);
    }
}
