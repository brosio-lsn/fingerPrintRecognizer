package cs107;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * You can use this class to test your program
 */
@SuppressWarnings("all")
public class Main {
    /**
     * Main entry point of the program.
     *
     * @param args the command lines arguments of the program.
     */
    public static void main(String[] args) {
        SignatureChecks.check();

        //bonus : almost all the tests have been improved and new ones were implemented

        // Tested and Passing
        testGetNeighbours();
        testBlackNeighbours();
        testTransitions();
        testIdentical();
        testThinningStep();
        testConnectedPixels();
        testSpreadPixel();
        testSubClone();
        testMatchingMinutiaeCount();

        // passing but more tests recommended
        testThin();
        testComputeSlope();
        testComputeAngle();
        testComputeOrientation();
        testApplyRotation();
        testApplyTranslation();

        //testExtract();

        var bestConstants = bestConstants();
        System.out.println(
            "Best constants: "
                + "orientationDistance: "
                + bestConstants.get("orientationDistance")
                + "distanceThreshold: "
                + bestConstants.get("distanceThreshold")
                + "foundThreshold: "
                + bestConstants.get("foundThreshold")
                + "orientationThreshold: "
                + bestConstants.get("orientationThreshold")
                + "matchAngleOffset: "
                + bestConstants.get("matchAngleOffset")
                + "percentage: "
                + bestConstants.get("percentage")
        );

        testRandomComparisons();
        testSuccessComparisons();
        testFailComparisons();
    }

    // bonus : try to brute force the best constants
    public static HashMap<String, Integer> bestConstants() {
        // setting the best combination of constants so that the below intervals are correct
        final var initial = new HashMap<String, Integer>();
        initial.put("orientationDistance", 12);
        initial.put("distanceThreshold", 1);
        initial.put("foundThreshold", 16);
        initial.put("orientationThreshold", 16);
        initial.put("matchAngleOffset", 0);
        initial.put("percentage", 0);

        var best = new HashMap<String, Integer>();

        IntStream
            .range(0, 8)
            .map(orientationDistance -> orientationDistance + initial.get("orientationDistance"))
            .forEach(orientationDistance -> IntStream
                .range(0, 8)
                .map(distanceThreshold -> distanceThreshold + initial.get("distanceThreshold"))
                .forEach(distanceThreshold -> IntStream
                    .range(0, 8)
                    .map(foundThreshold -> foundThreshold + initial.get("foundThreshold"))
                    .forEach(foundThreshold -> IntStream
                        .range(0, 8)
                        .map(orientationThreshold -> orientationThreshold + initial.get("orientationThreshold"))
                        .forEach(orientationThreshold -> IntStream
                            .range(0, 8)
                            .map(matchAngleOffset -> matchAngleOffset + initial.get("matchAngleOffset"))
                            .forEach(matchAngleOffset -> {
                                Fingerprint.ORIENTATION_DISTANCE = initial.get("orientationDistance") + orientationDistance;
                                Fingerprint.DISTANCE_THRESHOLD = initial.get("distanceThreshold") + distanceThreshold;
                                Fingerprint.FOUND_THRESHOLD = initial.get("foundThreshold") + foundThreshold;
                                Fingerprint.ORIENTATION_THRESHOLD = initial.get("orientationThreshold") + orientationThreshold;
                                Fingerprint.MATCH_ANGLE_OFFSET = initial.get("matchAngleOffset") + matchAngleOffset;

                                // run a total of 8*10 tests
                                int totalPercentage = IntStream
                                    .range(0, 10)
                                    .map(n -> testRandomComparisons())
                                    .sum();
                                // the percentage of correct matches we have with the currently tested constant's value
                                totalPercentage = totalPercentage / 80;
                                // if a better combination is found change our answer list "best"
                                if (totalPercentage > initial.get("percentage")) {
                                    best.put("orientationDistance", orientationDistance);
                                    best.put("distanceThreshold", distanceThreshold);
                                    best.put("foundThreshold", foundThreshold);
                                    best.put("orientationThreshold", orientationThreshold);
                                    best.put("matchAngleOffset", matchAngleOffset);
                                    best.put("percentage", totalPercentage);
                                }
                            })))));
        return best;
    }

    // General testing utilities
    public static int testRandomComparisons() {
        var randomCheck = new Random()
            .ints(1, 17)
            .limit(4).parallel() // for each fingerprint
            .mapToLong(f1 -> new Random()
                .ints(1, 17)
                .limit(1).parallel() // go through each subsequent fingerprint
                .mapToLong(f2 -> new Random()
                    .ints(1, 9)
                    .limit(2).parallel() // go through each version of the first fingerprint
                    .mapToLong(v1 -> new Random()
                        .ints(1, 9)
                        .limit(1).parallel() // and each version of the second fingerprint
                        .mapToObj(v2 -> testCompareFingerprints( // compare them, and don't expect them to match
                            f1 + "_" + v1,
                            f2 + "_" + v2,
                            f1 == f2))
                        .filter(i -> i) // keep all the correct results
                        .count()) // count them
                    .sum())
                .sum())
            .sum();
        System.out.println(randomCheck + " / 8");
        return ((int) randomCheck);
    }

    public static void testSuccessComparisons() {
        long successTests = IntStream
            .range(1, 17).parallel() // for each fingerprint
            .mapToLong(f1 -> IntStream
                .range(1, 9).parallel() // go through each version of the fingerprint
                .mapToLong(v1 -> IntStream
                    .range(v1 + 1, 9).parallel() // go through subsequent version of the fingerprint
                    .mapToObj(v2 -> testCompareFingerprints( // compare them and expect them to match
                        f1 + "_" + v1,
                        f1 + "_" + v2,
                        true))
                    .filter(i -> i) // keep all of the correct results
                    .count()) // count them
                .sum())
            .sum(); // count how many overall were as expected
        System.out.println("Intended successes: " + successTests);
    }

    public static void testFailComparisons() {
        long failTests = IntStream
            .range(1, 17).parallel() // for each fingerprint
            .mapToLong(f1 -> IntStream
                .range(f1 + 1, 17).parallel() // go through each subsequent fingerprint
                .mapToLong(f2 -> IntStream
                    .range(1, 9).parallel() // go through each version of the first fingerprint
                    .mapToLong(v1 -> IntStream
                        .range(1, 9).parallel() // and each version of the second fingerprint
                        .mapToObj(v2 -> testCompareFingerprints( // compare them, and don't expect them to match
                            f1 + "_" + v1,
                            f2 + "_" + v2,
                            false))
                        .filter(i -> i) // keep all the correct results
                        .count()) // count them
                    .sum())
                .sum())
            .sum(); // count how many overall were as expected
        System.out.println("Intended failures: " + failTests);
    }


    public static void testGetNeighbours() {
        {
            System.out.print("test GetNeighbours 1: ");
            boolean[][] image = {{true}};
            boolean[] neighbours = Fingerprint.getNeighbours(image, 0, 0);
            boolean[] expected = {false, false, false, false,
                false, false, false, false};
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 2: ");
            boolean[][] image = {{true, true}};
            boolean[] neighbours = Fingerprint.getNeighbours(image, 0, 0);
            boolean[] expected = {false, false, true, false, false, false, false, false};
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 3: ");
            boolean[][] image = {
                {true, true, true},
                {true, true, true},
                {true, true, true},
            };
            boolean[] neighbours = Fingerprint.getNeighbours(image, 1, 1);
            boolean[] expected = {true, true, true, true, true, true, true, true};
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 4: ");
            boolean[][] image = {{}};
            boolean[] neighbours = Fingerprint.getNeighbours(image, 0, 0);
            boolean[] expected = null;
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 5: ");
            boolean[][] image = {
                {true, true, true},
                {true, true, true},
                {true, true, true}
            };
            boolean[] neighbours = Fingerprint.getNeighbours(image, 3, 3);
            boolean[] expected = null;
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 6: ");
            boolean[][] image = {
                {true, true, true},
                {true, true, true},
                {true, true, true}
            };
            boolean[] neighbours = Fingerprint.getNeighbours(image, 1, 1);
            boolean[] expected = {true, true, true, true, true, true, true, true};
            image = new boolean[][]{
                {false, false, false},
                {false, false, false},
                {false, false, false}
            };
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
        {
            System.out.print("test GetNeighbours 7: ");
            boolean[][] image = {
                {false, true, true},
                {false, true, false},
                {false, false, false}
            };
            boolean[] neighbours = Fingerprint.getNeighbours(image, 1, 1);
            boolean[] expected = {true, true, false, false, false, false, false, false};
            image = new boolean[][]{
                {false, true, true},
                {false, true, false},
                {false, false, false}
            };
            if (arrayEqual(neighbours, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, neighbours);
            }
        }
    }

    public static void testBlackNeighbours() {
        {
            System.out.print("test BlackNeighbours 1: ");
            boolean[] neighbors = {true};
            int blackNeighbours = Fingerprint.blackNeighbours(neighbors);
            int expected = 1;
            if (blackNeighbours == expected) {
                System.out.println("OK");
            } else {
                printError(expected, blackNeighbours);
            }
        }
        {
            System.out.print("test BlackNeighbours 2: ");
            boolean[] neighbors = {false};
            int blackNeighbours = Fingerprint.blackNeighbours(neighbors);
            int expected = 0;
            if (blackNeighbours == expected) {
                System.out.println("OK");
            } else {
                printError(expected, blackNeighbours);
            }
        }
        {
            System.out.print("test BlackNeighbours 3: ");
            boolean[] neighbors = {};
            int blackNeighbours = Fingerprint.blackNeighbours(neighbors);
            int expected = 0;
            if (blackNeighbours == expected) {
                System.out.println("OK");
            } else {
                printError(expected, blackNeighbours);
            }
        }
        {
            System.out.print("test BlackNeighbours 4: ");
            boolean[] neighbors = {true, false, true, false, true, false, true, false};
            int blackNeighbours = Fingerprint.blackNeighbours(neighbors);
            int expected = 4;
            if (blackNeighbours == expected) {
                System.out.println("OK");
            } else {
                printError(expected, blackNeighbours);
            }
        }
    }

    public static void testTransitions() {
        {
            System.out.print("test Transitions 1: ");
            boolean[] neighbours = {true, false, true, false, false};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 2;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 2: ");
            boolean[] neighbours = {true, true, false, false, false, false, false, false};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 1;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 3: ");
            boolean[] neighbours = {false, false, false, false, false};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 0;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 4: ");
            boolean[] neighbours = {true, true, true, true, true};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 0;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 5: ");
            boolean[] neighbours = {false};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 0;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 6: ");
            boolean[] neighbours = {true};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 0;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 7: ");
            boolean[] neighbours = {};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 0;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
        {
            System.out.print("test Transitions 8: ");
            boolean[] neighbours = {true, false, true, false, true};
            int transitions = Fingerprint.transitions(neighbours);
            int expected = 2;
            if (transitions == expected) {
                System.out.println("OK");
            } else {
                printError(expected, transitions);
            }
        }
    }

    public static void testIdentical() {
        {
            System.out.print("test Identical 1: ");
            boolean[][] x = {
                {true, false},
                {false, true}
            };
            boolean[][] y = {
                {true, false},
                {false, true}
            };
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = true;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 2: ");
            boolean[][] x = null;
            boolean[][] y = null;
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = true;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 3: ");
            boolean[][] x = {
                {true, false},
                {false, true}
            };
            boolean[][] y = null;
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = false;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 4: ");
            boolean[][] x = {
                {true, false},
                {false, true},
                {false, false}
            };
            boolean[][] y = {
                {true, false},
                {false, true}
            };
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = false;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 5: ");
            boolean[][] x = {
                {true, false, true},
                {false, true, false}
            };
            boolean[][] y = {
                {true, false},
                {false, true}
            };
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = false;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 6: ");
            boolean[][] x = {
                {true, false},
                {false, true}
            };
            boolean[][] y = {
                {true, false},
                {false, false}
            };
            boolean identical = Fingerprint.identical(x, y);
            boolean expected = false;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
        {
            System.out.print("test Identical 7: ");
            boolean[][] x = {
                {true, false},
                {false, true}
            };
            boolean[][] y = {
                {true, false},
                {false, true}
            };
            boolean identical = Fingerprint.identical(x, y);
            y = new boolean[][]{
                {false, false},
                {false, true}
            };
            boolean expected = true;
            if (identical == expected) {
                System.out.println("OK");
            } else {
                printError(expected, identical);
            }
        }
    }

    public static void testThinningStep() {
        {
            System.out.print("test ThinningStep 1: ");
            boolean[][] image = {
                {false, false, true, true},
                {false, false, true, false},
                {false, false, false, false},
                {false, false, false, false}
            };
            boolean[][] expected = {
                {false, false, true, false},
                {false, false, false, false},
                {false, false, false, false},
                {false, false, false, false}
            };
            boolean[][] thinningStep = Fingerprint.thinningStep(image, 0);
            if (Fingerprint.identical(thinningStep, expected))
                System.out.println("OK");
            else {
                printError(expected, thinningStep);
            }
        }
        {
            System.out.print("test ThinningStep 2: ");
            boolean[][] image = {
                {false, false, false, false},
                {false, false, false, false},
                {true, true, false, false},
                {true, false, true, false}
            };
            boolean[][] expected = {
                {false, false, false, false},
                {false, false, false, false},
                {true, true, false, false},
                {false, false, true, false}
            };
            boolean[][] thinningStep = Fingerprint.thinningStep(image, 0);
            if (Fingerprint.identical(thinningStep, expected))
                System.out.println("OK");
            else {
                printError(expected, thinningStep);
            }
        }
        {
            System.out.print("test ThinningStep 3: ");
            boolean[][] image = {
                {false, false, false, false},
                {false, false, false, false},
                {true, true, false, false},
                {true, false, false, false}
            };
            boolean[][] expected = {
                {false, false, false, false},
                {false, false, false, false},
                {true, false, false, false},
                {false, false, false, false}
            };
            boolean[][] thinningStep = Fingerprint.thinningStep(image, 1);
            if (Fingerprint.identical(thinningStep, expected))
                System.out.println("OK");
            else {
                printError(expected, thinningStep);
            }
        }
        {
            System.out.print("test ThinningStep 4: ");
            boolean[][] image = {
                {false, false, true, true},
                {false, false, true, false},
                {false, false, false, false},
                {false, false, false, false}
            };
            boolean[][] expected = {
                {false, false, true, false},
                {false, false, false, false},
                {false, false, false, false},
                {false, false, false, false}
            };
            boolean[][] thinningStep = Fingerprint.thinningStep(image, 0);
            if (Fingerprint.identical(thinningStep, expected))
                System.out.println("OK");
            else {
                printError(expected, thinningStep);
            }
        }
    }

    public static void testConnectedPixels() {
        {
            System.out.print("test ConnectedPixels 1: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = new boolean[21][21];
            expected[10][10] = true;
            expected[10][11] = true;
            expected[9][11] = true;
            expected[9][12] = true;
            expected[8][12] = true;
            boolean[][] connectedPixels = Fingerprint.connectedPixels(image, 2, 1, 10);
            if (arrayEqual(connectedPixels, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, connectedPixels);
            }
        }
        {
            System.out.print("test ConnectedPixels 2: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = {
                {false, false, true},
                {false, true, true},
                {false, false, false}
            };
            boolean[][] connectedPixels = Fingerprint.connectedPixels(image, 2, 1, 1);
            if (arrayEqual(connectedPixels, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, connectedPixels);
            }
        }
        {
            System.out.print("test ConnectedPixels 3: ");
            boolean[][] image = {
                {true, false, false, true, true},
                {true, false, true, true, false},
                {true, true, false, false, false},
                {false, true, false, true, false}
            };
            boolean[][] expected = {
                {false, true, false, false, true},
                {false, true, false, true, true},
                {false, true, true, false, false},
                {false, false, true, false, false},
                {false, false, false, false, false}
            };
            boolean[][] connectedPixels = Fingerprint.connectedPixels(image, 2, 1, 2);
            if (arrayEqual(connectedPixels, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, connectedPixels);
            }
        }
    }

    public static void testSpreadPixel() {
        {
            System.out.print("test SpreadPixel 1: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, true, false, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            int squareSideLength = 3;
            int col = 1;
            int row = 1;
            boolean[][] clone = Fingerprint.subClone(image, 0, 0, squareSideLength);
            boolean[][] relevant = new boolean[squareSideLength][squareSideLength];
            relevant[row][col] = image[row][col];
            boolean[][] expected = new boolean[squareSideLength][squareSideLength];
            expected[1][1] = true;
            expected[0][0] = true;
            expected[2][1] = true;
            expected[2][2] = true;
            Fingerprint.spreadCentrePixel(clone, relevant, row, col);
            if (arrayEqual(relevant, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, relevant);
            }
        }
        {
            System.out.print("test SpreadPixel 2: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, false, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            int squareSideLength = 3;
            int col = 1;
            int row = 1;
            boolean[][] clone = Fingerprint.subClone(image, 0, 0, squareSideLength);
            boolean[][] relevant = new boolean[squareSideLength][squareSideLength];
            relevant[row][col] = image[row][col];
            boolean[][] expected = new boolean[squareSideLength][squareSideLength];
            Fingerprint.spreadCentrePixel(clone, relevant, row, col);
            if (arrayEqual(relevant, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, relevant);
            }
        }
        {
            System.out.print("test SpreadPixel 3: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, true, false, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            int squareSideLength = 3;
            int col = 0;
            int row = 0;
            boolean[][] clone = Fingerprint.subClone(image, 0, 0, squareSideLength);
            boolean[][] relevant = new boolean[squareSideLength][squareSideLength];
            relevant[row][col] = image[row][col];
            boolean[][] expected = new boolean[squareSideLength][squareSideLength];
            expected[0][0] = true;
            expected[1][1] = true;
            Fingerprint.spreadCentrePixel(clone, relevant, row, col);
            if (arrayEqual(relevant, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, relevant);
            }
        }
    }

    public static void testSubClone() {
        {
            System.out.print("test SubClone1: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = new boolean[21][21];
            expected[0][0] = true;
            expected[0][3] = true;
            expected[1][2] = true;
            expected[1][3] = true;
            expected[2][1] = true;
            expected[2][2] = true;
            boolean[][] clone = Fingerprint.subClone(image, 0, 0, 21);
            if (arrayEqual(clone, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, clone);
            }
        }
        {
            System.out.print("test SubClone2: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = new boolean[21][21];
            expected[0][0] = true;
            expected[0][1] = true;
            boolean[][] clone = Fingerprint.subClone(image, 2, 1, 21);
            if (arrayEqual(clone, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, clone);
            }
        }
        {
            System.out.print("test SubClone3: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = new boolean[21][21];
            expected[10][10] = true;
            expected[10][13] = true;
            expected[11][12] = true;
            expected[11][13] = true;
            expected[12][11] = true;
            expected[12][12] = true;
            boolean[][] clone = Fingerprint.subClone(image, -10, -10, 21);
            if (arrayEqual(clone, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, clone);
            }
        }
        {
            System.out.print("test SubClone4: ");
            boolean[][] image = {
                {true, false, false, true},
                {false, false, true, true},
                {false, true, true, false},
                {false, false, false, false}
            };
            boolean[][] expected = new boolean[0][0];
            boolean[][] clone = Fingerprint.subClone(image, 0, 0, 0);
            if (arrayEqual(clone, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, clone);
            }
        }
    }

    public static void testComputeSlope() {
        {
            boolean[][] image = {
                {false, false, false, true, false},
                {false, false, true, true, false},
                {false, true, true, false, false},
                {false, false, false, false, false}
            };
            double expected = 0.7;
            double slope = Fingerprint.computeSlope(image, 2, 1);
            if (slope == expected) {
                System.out.println("test ComputeSlope 1: OK");
            } else {
                printError(expected, slope);
            }
        }
    }

    public static void testComputeAngle() {
        {
            boolean[][] image = {
                {false, false, false, true, false},
                {false, false, true, true, false},
                {false, true, true, false, false},
                {false, false, false, false, false}
            };
            double expected = Math.atan(0.7);
            double slope = Fingerprint.computeSlope(image, 2, 1);
            double angle = Fingerprint.computeAngle(image, 2, 1, slope);
            if (angle == expected) {
                System.out.println("test ComputeAngle 1: OK");
            } else {
                printError(expected, slope);
            }
        }
    }

    public static void testExtract() {
        {
            System.out.print("test Extract 1: ");
            boolean[][] image = {
                {false, false, false, true, false},
                {false, false, true, true, false},
                {false, true, true, false, false},
                {false, false, false, false, false}
            };
            var expected = new ArrayList<int[]>();
            expected.add(new int[]{2, 1, 270});

            var minutiaes = Fingerprint.extract(image);

            if (minutiaes.equals(expected)) {
                System.out.println("OK");
            } else {
                printError(expected, minutiaes);
            }
        }
    }

    public static void testComputeOrientation() {
        {
            System.out.print("test ComputeOrientation 1: ");
            boolean[][] image = {
                {false, false, false, true, false},
                {false, false, true, true, false},
                {false, true, true, false, false},
                {false, false, false, false, false}
            };
            double expected = Math.round(Math.toDegrees(Math.atan(0.7)));
            double angle = Fingerprint.computeOrientation(image, 2, 1, 30);
            if (angle == expected) {
                System.out.println("OK");
            } else {
                printError(expected, angle);
            }
        }
    }

    public static void testApplyRotation() {
        var minutia = new int[]{1, 3, 10};
        int[] result;
        {
            // minutia, centerRow, centerCol, rotation)
            result = Fingerprint.applyRotation(minutia, 0, 0, 0);
            var expected = new int[]{1, 3, 10};
            System.out.print("test applyRotation 1: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
        {
            result = Fingerprint.applyRotation(minutia, 10, 5, 0);
            var expected = new int[]{1, 3, 10};
            System.out.print("test applyRotation 2: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
        {
            result = Fingerprint.applyRotation(minutia, 0, 0, 90);
            var expected = new int[]{-3, 1, 100};
            System.out.print("test applyRotation 3: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
        {
            result = Fingerprint.applyRotation(new int[]{0, 3, 10}, 0, 0, 90);
            var expected = new int[]{-3, 0, 100};
            System.out.print("test applyRotation 4: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
        {
            result = Fingerprint.applyRotation(new int[]{3, 0, 10}, 0, 0, 90);
            var expected = new int[]{0, 3, 100};
            System.out.print("test applyRotation 5: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
    }

    public static void testApplyTranslation() {
        var minutia = new int[]{1, 3, 10};
        int[] result;
        {
            result = Fingerprint.applyTranslation(minutia, 0, 0);
            var expected = new int[]{1, 3, 10};
            System.out.print("test applyTranslation 1: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
        {
            result = Fingerprint.applyTranslation(minutia, 10, 5);
            var expected = new int[]{-9, -2, 10};
            System.out.print("test applyTranslation 2: ");
            if (Arrays.equals(result, expected)) {
                System.out.println("OK");
            } else {
                printError(expected, result);
            }
        }
    }

    public static void testApplyTransformation() {
        // TODO test this
    }

    public static void testMatchingMinutiaeCount() {
        {
            ArrayList<int[]> minutiae1 = new ArrayList<>();
            minutiae1.add(new int[]{1, 50, 36});
            minutiae1.add(new int[]{5, 25, 96});
            ArrayList<int[]> minutiae2 = new ArrayList<>();
            minutiae2.add(new int[]{1, 50, 36});
            minutiae2.add(new int[]{5, 25, 96});
            int result = Fingerprint.matchingMinutiaeCount(minutiae1, minutiae2, Fingerprint.DISTANCE_THRESHOLD, Fingerprint.ORIENTATION_DISTANCE);
            int expected = 2;
            if (result == expected) {
                System.out.println("test MatchingMinutiaeCount 1 : OK");
            } else {
                printError(expected, result);
            }
        }

        {
            ArrayList<int[]> minutiae1 = new ArrayList<>();
            minutiae1.add(new int[]{150, 50, 36});
            minutiae1.add(new int[]{5, 25, 96});
            minutiae1.add(new int[]{15, 32, 109});
            ArrayList<int[]> minutiae2 = new ArrayList<>();
            minutiae2.add(new int[]{151, 50, 37});
            minutiae2.add(new int[]{5, 25, 96});
            minutiae2.add(new int[]{60, 32, 192});
            int result = Fingerprint.matchingMinutiaeCount(minutiae1, minutiae2, Fingerprint.DISTANCE_THRESHOLD, Fingerprint.ORIENTATION_DISTANCE);
            int expected = 2;
            if (result == expected) {
                System.out.println("test MatchingMinutiaeCount 2 : OK");
            } else {
                printError(expected, result);
            }
        }
    }

    /**
     * This function is here to help you test the functionalities of extract.
     * It will read the first fingerprint and extract the minutiae. It will save
     * the thinned version as skeleton_1_1.png and a version where the minutiae
     * are drawn on top as minutiae_1_1.png. You are free to modify and/or delete
     * it.
     */
    public static void testThin() {
        {
            boolean[][] image1 = Helper.readBinary("src/resources/test_inputs/1_1_small.png");
            boolean[][] expected = Helper.readBinary("src/resources/test_outputs/skeleton_1_1_small.png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            if (Fingerprint.identical(expected, skeleton1)) {
                System.out.println("test thin 1: OK");
            } else {
                printError(expected, skeleton1);
            }
        }
        {
            boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/1_1.png");
            boolean[][] expected = Helper.readBinary("src/resources/test_outputs/skeleton_1_1.png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            if (Fingerprint.identical(expected, skeleton1)) {
                System.out.println("test thin 2: OK");
            } else {
                printError(expected, skeleton1);
            }
        }
        {
            boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/1_2.png");
            boolean[][] expected = Helper.readBinary("src/resources/test_outputs/skeleton_1_2.png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            if (Fingerprint.identical(expected, skeleton1)) {
                System.out.println("test thin 3: OK");
            } else {
                printError(expected, skeleton1);
            }
        }
        {
            boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/2_1.png");
            boolean[][] expected = Helper.readBinary("src/resources/test_outputs/skeleton_2_1.png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            if (Fingerprint.identical(expected, skeleton1)) {
                System.out.println("test thin 4: OK");
            } else {
                printError(expected, skeleton1);
            }
        }
    }

    public static void testDrawSkeleton(String name) {
        {
            boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/" + name + ".png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            Helper.writeBinary("skeleton_" + name + ".png", skeleton1);
        }
    }

    public static void testDrawMinutiae(String name) {
        {
            boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/" + name + ".png");
            assert image1 != null;
            boolean[][] skeleton1 = Fingerprint.thin(image1);
            List<int[]> minutia1 = Fingerprint.extract(skeleton1);
            int[][] colorImageSkeleton1 = Helper.fromBinary(skeleton1);
            Helper.drawMinutia(colorImageSkeleton1, minutia1);
            Helper.writeARGB("minutiae_" + name + ".png", colorImageSkeleton1);
        }
    }

    /**
     * This function is here to help you test the functionalities of extract
     * without using the function thin. It will read the first fingerprint and
     * extract the minutiae. It will save a version where the minutiae are drawn
     * on top as minutiae_skeletonTest.png. You are free to modify and/or delete
     * it.
     */
    public static void testWithSkeleton() {
        {
            boolean[][] skeleton1 = Helper.readBinary("src/resources/test_inputs/skeletonTest.png");
            List<int[]> minutiae1 = Fingerprint.extract(skeleton1);
            List<int[]> expected = new ArrayList<>();
            expected.add(new int[]{39, 21, 264});
            expected.add(new int[]{53, 33, 270});

            System.out.print("Expected minutiae: ");
            printMinutiae(expected);
            System.out.print("Computed minutiae: ");
            printMinutiae(minutiae1);

            // Draw the minutiae on top of the thinned image
            assert skeleton1 != null;
            int[][] colorImageSkeleton1 = Helper.fromBinary(skeleton1);
            Helper.drawMinutia(colorImageSkeleton1, minutiae1);
            Helper.writeARGB("minutiae_skeletonTest.png", colorImageSkeleton1);
        }
    }

    /**
     * This function is here to help you test the overall functionalities. It will
     * compare the fingerprint in the file name1.png with the fingerprint in the
     * file name2.png. The third parameter indicates if we expected a match or not.
     */
    public static boolean testCompareFingerprints(String name1, String name2, boolean expectedResult) {
        boolean[][] image1 = Helper.readBinary("src/resources/fingerprints/" + name1 + ".png");
        assert image1 != null;
        boolean[][] skeleton1 = Fingerprint.thin(image1);
        List<int[]> minutiae1 = Fingerprint.extract(skeleton1);

        boolean[][] image2 = Helper.readBinary("src/resources/fingerprints/" + name2 + ".png");
        assert image2 != null;
        boolean[][] skeleton2 = Fingerprint.thin(image2);
        List<int[]> minutiae2 = Fingerprint.extract(skeleton2);

        boolean isMatch = Fingerprint.match(minutiae1, minutiae2);
        var result = new StringBuilder();
        result.append("Compare ").append(name1).append(" with ").append(name2);
        if (isMatch == expectedResult)
            result.append(". OK!");
        else {
            result.append(". Expected match: ").append(expectedResult);
            result.append(" Computed match: ").append(isMatch);
        }
        System.out.println(result);
        return isMatch == expectedResult;
    }

    /**
     * This function is here to help you test the overall functionalities. It will
     * compare the fingerprint in the file <code>name1.png</code> with all the eight
     * fingerprints of the given finger (second parameter).
     * The third parameter indicates if we expected a match or not.
     */
    public static int testCompareAllFingerprints(String name1, int finger, boolean expectedResult) {
        return (int) IntStream.range(1, 9).parallel()
            .mapToObj(i -> testCompareFingerprints(name1, finger + "_" + i, expectedResult))
            .filter(i -> i)
            .count();
    }


    public static void testPrintError() {
        printError(true, false);

        boolean[] arr1 = {true, false};
        boolean[] arr2 = {false, true};
        printError(arr1, arr2);

        boolean[][] dArr1 = {{true, false}, {false, true}};
        boolean[][] dArr2 = {{false, true}, {false, true}};
        printError(dArr1, dArr2);

        int int1 = 1;
        int int2 = 2;
        printError(int1, int2);

        double double1 = 1;
        double double2 = 2;
        printError(double1, double2);

        List<int[]> listIntArr1 = new ArrayList<>();
        listIntArr1.add(new int[]{2, 4});
        listIntArr1.add(new int[]{5, 6});
        List<int[]> listIntArr2 = new ArrayList<>();
        listIntArr2.add(new int[]{4, 1});
        listIntArr2.add(new int[]{6, 8});
        printError(listIntArr1, listIntArr2);
    }

    public static void printError(int[] expected, int[] computed) {
        printError(Arrays.toString(expected), Arrays.toString(computed));
    }

    public static void printError(boolean[] expected, boolean[] computed) {
        printError(Arrays.toString(expected), Arrays.toString(computed));
    }

    public static void printError(boolean[][] expected, boolean[][] computed) {
        var expect = Arrays.stream(expected)
            .map(Arrays::toString)
            .map(i -> i + "\n")
            .collect(Collectors.toList());
        var compute = Arrays.stream(computed)
            .map(Arrays::toString)
            .map(i -> i + "\n")
            .collect(Collectors.toList());
        var str = new StringBuilder();
        str.append("ERROR\n")
            .append("Expected: \n");
        expect.forEach(str::append);
        str.append("Computed: \n");
        compute.forEach(str::append);
        System.out.println(str);
    }

    public static void printError(List<int[]> expected, List<int[]> computed) {
        System.out.println("ERROR");
        System.out.println("Expected: " + expected
            .stream()
            .map(Arrays::toString)
            .collect(Collectors.joining(", "))
        );
        System.out.println("Computed: " + computed
            .stream()
            .map(Arrays::toString)
            .collect(Collectors.joining(", "))
        );
    }

    public static <T> void printError(T expected, T computed) {
        System.out.println("ERROR");
        System.out.println("Expected: " + expected);
        System.out.println("Computed: " + computed);
    }


    /*
     * Helper functions to print and compare arrays
     *
     * UTIL
     *
     * NOT TESTS
     */
    public static boolean arrayEqual(boolean[] array1, boolean[] array2) {
        if (array1 == null && array2 == null)
            return true;
        if (array1 == null || array2 == null)
            return false;
        if (array1.length != array2.length)
            return false;

        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i])
                return false;
        }
        return true;
    }

    public static boolean arrayEqual(boolean[][] array1, boolean[][] array2) {
        if (array1 == null && array2 == null)
            return true;
        if (array1 == null || array2 == null)
            return false;
        if (array1.length != array2.length)
            return false;

        for (int i = 0; i < array1.length; i++) {
            if (!arrayEqual(array1[i], array2[i]))
                return false;
        }
        return true;
    }

    public static void printMinutiae(List<int[]> minutiae) {
        for (int[] minutia : minutiae) {
            System.out.print("[");
            for (int j = 0; j < minutia.length; j++) {
                System.out.print(minutia[j]);
                if (j != minutia.length - 1)
                    System.out.print(", ");
            }
            System.out.println("],");
        }
    }
}
