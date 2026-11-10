package com.dozingcatsoftware.vectorpinball.fields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Stars {

    private Stars() {}

    static double greatCircleDistance(double ra1, double dec1, double ra2, double dec2) {
        // https://en.wikipedia.org/wiki/Great-circle_distance
        return Math.acos(sin(dec1) * sin(dec2) + cos(dec1) * cos(dec2) * cos(ra1 - ra2));
    }

    static class StarCatalog {
        double[] decRadians;
        double[] raRadians;
        double[] x;
        double[] y;
        double[] z;
        double[] magnitude;

        int size() {
            return this.x.length;
        }
    }

    static StarCatalog CATALOG = makeCatalog(StarData.STAR_DATA);

    private static StarCatalog makeCatalog(double[] starData) {
        StarCatalog cat = new StarCatalog();
        assert starData.length % 3 == 0;
        int numStars = starData.length / 3;
        cat.decRadians = new double[numStars];
        cat.raRadians = new double[numStars];
        cat.x = new double[numStars];
        cat.y = new double[numStars];
        cat.z = new double[numStars];
        cat.magnitude = new double[numStars];
        for (int i = 0; i < numStars; i++) {
            int offset = 3 * i;
            double rho = Math.toRadians(starData[offset]);
            double theta = Math.toRadians(starData[offset + 1]);
            cat.decRadians[i] = rho;
            cat.raRadians[i] = theta;
            cat.x[i] = cos(rho) * cos(theta);
            cat.y[i] = -cos(rho) * sin(theta);
            cat.z[i] = sin(rho);
            cat.magnitude[i] = starData[offset + 2];
        }
        return cat;
    }

    // Constallations from https://www.iau.org/public/themes/constellations/
    // Mapped to star indices from Stars.STAR_DATA.
    static List<Constellation> CONSTELLATIONS = Arrays.asList(
            Constellation.withSegments(CATALOG, "Canis Major",
                    0, 785, 580, 785, 566, 580, 566, 785, 0, 47, 0, 37, 47, 494, 461, 494, 23, 461,
                    23, 37, 37, 90),
            Constellation.withSegments(CATALOG, "Gemini",
                    17, 555, 317, 555, 401, 555, 401, 817, 326, 817, 45, 817,
                    298, 555, 298, 320, 248, 320, 298, 519, 43, 519,
                    187, 817, 187, 601, 146, 187),
            Constellation.withSegments(CATALOG, "Leo",
                    22, 281, 50, 281, 50, 269, 269, 454, 167, 454, 167, 167, 281, 167, 167, 737, 737, 879,
                    454, 879, 50, 97, 63, 97, 63, 241, 97, 241, 241, 281, 241, 516, 516, 548),
            Constellation.withSegments(CATALOG, "Orion",
                    5, 247, 74, 247, 58, 31, 29, 31, 29, 74, 7, 26, 7, 31, 7, 253, 26, 74, 26, 253,
                    7, 594, 594, 823, 809, 823, 823, 1053, 809, 1053,
                    26, 212, 212, 351, 351, 365, 212, 768, 768, 1052, 551, 1052),
            Constellation.withSegments(CATALOG, "Sagittarius",
                    53, 240, 101, 240, 53, 205, 101, 205, 35, 101, 35, 117, 35, 168, 117, 168, 35, 190,
                    205, 117, 134, 205, 117, 134, 134, 432),
            Constellation.withSegments(CATALOG, "Ursa Major",
                    36, 82, 82, 86, 36, 233, 86, 233, 233, 33, 33, 73, 38, 73,
                    36, 348, 82, 402, 348, 402, 245, 348, 245, 402,
                    86, 357, 283, 357, 283, 743, 176, 357, 176, 188, 188, 273,
                    204, 402, 204, 315, 200, 315)
    );

    static class Constellation {
        String name;
        Set<Integer> starIndices;
        double centerRaRadians;
        double centerDecRadians;
        double angularRadius;
        // Flattened pairs of star indices.
        int[] segments;
        // Maps smaller index to endpoints with greater indices.
        Map<Integer, Set<Integer>> segmentsByIndex = new HashMap<>();

        static Constellation withSegments(StarCatalog catalog, String name, int... segments) {
            assert segments.length % 2 == 0;
            Constellation self = new Constellation();
            self.name = name;
            self.segments = segments;

            Set<Integer> starIndices = new HashSet<>();
            for (int i = 0; i < segments.length; i += 2) {
                int a = segments[i];
                int b = segments[i + 1];
                int minIndex = Math.min(a, b);
                if (!self.segmentsByIndex.containsKey(minIndex)) {
                    self.segmentsByIndex.put(minIndex, new HashSet<>());
                }
                self.segmentsByIndex.get(minIndex).add(Math.max(a, b));
                starIndices.add(a);
                starIndices.add(b);
            }

            double minRa = catalog.raRadians[segments[0]];
            double maxRa = minRa;
            double minDec = catalog.decRadians[segments[0]];
            double maxDec = minDec;
            double sumRa = 0;
            double sumDec = 0;
            for (int index : starIndices) {
                double ra = catalog.raRadians[index];
                double dec = catalog.decRadians[index];
                sumRa += ra;
                sumDec += dec;
                minRa = Math.min(minRa, ra);
                maxRa = Math.max(maxRa, ra);
                minDec = Math.min(minDec, dec);
                maxDec = Math.max(maxDec, dec);
            }
            double avgRa = sumRa / starIndices.size();
            double avgDec = sumDec / starIndices.size();
            double maxDist = 0;
            for (int index : starIndices) {
                double ra = catalog.raRadians[index];
                double dec = catalog.decRadians[index];
                // https://en.wikipedia.org/wiki/Great-circle_distance
                double dist = greatCircleDistance(ra, dec, avgRa, avgDec);
                maxDist = Math.max(maxDist, dist);
            }
            self.centerRaRadians = avgRa;
            self.centerDecRadians = avgDec;
            self.angularRadius = maxDist;
            self.starIndices = starIndices;
            return self;
        }
    }
}
