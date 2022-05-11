package cs107;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides tools to compare fingerprint.
 */
public class Fingerprint {

    /**
     * The number of pixels to consider in each direction when doing the linear
     * regression to compute the orientation.
     */
    public static int ORIENTATION_DISTANCE = 16;

    /**
     * The maximum distance between two minutiae to be considered matching.
     */
    public static int DISTANCE_THRESHOLD = 5;

    /**
     * The number of matching minutiae needed for two fingerprints to be considered
     * identical.
     */
    public static int FOUND_THRESHOLD = 20;

    /**
     * The distance between two angle to be considered identical.
     */
    public static int ORIENTATION_THRESHOLD = 20;

    /**
     * The offset in each direction for the rotation to test when doing the
     * matching.
     */
    public static int MATCH_ANGLE_OFFSET = 2;

    /**
     * Returns an array containing the value of the 8 neighbours of the pixel at
     * coordinates <code>(row, col)</code>.
     * <p>
     * The pixels are returned such that their indices corresponds to the following
     * diagram:<br>
     * ------------- <br>
     * | 7 | 0 | 1 | <br>
     * ------------- <br>
     * | 6 | _ | 2 | <br>
     * ------------- <br>
     * | 5 | 4 | 3 | <br>
     * ------------- <br>
     * <p>
     * If a neighbours is out of bounds of the image, it is considered white.
     * <p>
     * If the <code>row</code> or the <code>col</code> is out of bounds of the
     * image, the returned value should be <code>null</code>.
     *
     * @param image nested array containing each pixel's boolean value.
     * @param row   the row of the pixel of interest, must be between
     *              <code>0</code>(included) and
     *              <code>image.length</code>(excluded).
     * @param col   the column of the pixel of interest, must be between
     *              <code>0</code>(included) and
     *              <code>image[row].length</code>(excluded).
     * @return An array containing each neighbours' value.
     */
    public static boolean [] getNeighbours(boolean[][] image, int row, int col) {
        // special case that is not expected (the image is supposed to have been checked earlier)
        assert image != null;

        // check if pixel is in the image bounds
        if (row < 0
            || row >= image.length
            || col < 0
            || col >= image[0].length
        ) return null;

        // check which of the sides of the 3x3 around the pixel are inbounds
        // @formatter:off
        boolean topRowInImage       = (row > 0                    );
        boolean rightColumnInImage  = (col < (image[0].length - 1)); // get length of an inner list (# of col)
        boolean bottomRowInImage    = (row < (image.length    - 1));
        boolean leftColumnInImage   = (col > 0                    );

        // remember that positive y is down
        // for each pixel: if it's inbounds AND true, set the p-value to true
        return new boolean[]{ // p0 - p7
            topRowInImage                            && image[row - 1][col    ],
            topRowInImage      && rightColumnInImage && image[row - 1][col + 1],
            rightColumnInImage                       && image[row    ][col + 1],
            rightColumnInImage && bottomRowInImage   && image[row + 1][col + 1],
            bottomRowInImage                         && image[row + 1][col    ],
            bottomRowInImage   && leftColumnInImage  && image[row + 1][col - 1],
            leftColumnInImage                        && image[row    ][col - 1],
            leftColumnInImage  && topRowInImage      && image[row - 1][col - 1],
        };
        // @formatter:on
    }

    /**
     * Computes the number of black (<code>true</code>) pixels among the neighbours
     * of a pixel.
     *
     * @param neighbours array containing each pixel value. The array must respect
     *                   the convention described in
     *                   {@link #getNeighbours(boolean[][], int, int)}.
     * @return the number of black/<code>true</code> neighbours.
     */
    public static int blackNeighbours(boolean [] neighbours) {
        assert neighbours != null;
        return IntStream // accumulate how many neighbours are true
            .range(0, neighbours.length) // for each valid index of neighbours
            .map(index -> neighbours[index] ? 1 : 0) // if neighbours[index] is true return 1 and 0 if false
            .sum(); // sum how many 1s there are in the list
    }

    /**
     * Computes the number of white to black transitions among the neighbours of
     * pixel.
     *
     * @param neighbours array containing each pixel value. The array must respect
     *                   the convention described in
     *                   {@link #getNeighbours(boolean[][], int, int)}.
     * @return the number of white to black transitions.
     */
    public static int transitions(boolean [] neighbours) {
        assert neighbours != null;
        return IntStream
            .range(0, neighbours.length) // for each valid index of neighbours
            .map(index -> // turns every index into a 1 if neighbours[i] is false AND neighbours[index + 1] is true
                (!neighbours[index] && neighbours[(index + 1) % neighbours.length]) ? 1 : 0
            ) // (% allows index wrap around)
            .sum(); // sum how many 1s there are in the list
    }

    /**
     * Returns <code>true</code> if the images are identical and false otherwise.
     *
     * @param image1 array containing each pixel's boolean value.
     * @param image2 array containing each pixel's boolean value.
     * @return <code>True</code> if they are identical, <code>false</code>
     * otherwise.
     */
    public static boolean identical(boolean[][] image1, boolean[][] image2) {
        // @formatter:off
        return (image1 == null && image2 == null) // if both are null they are the same
            || ((image1 != null && image2 != null) // if one is null and the other isn't, return false
                && image1.length == image2.length // they have to be the same length
                && image1[0].length == image2[0].length // in both dimensions
                && IntStream
                    .range(0, image1.length) // for each valid index of image (rows)
                    .noneMatch( // make sure that all rows have no differing pixels
                        row -> IntStream
                            .range(0, image1[0].length) // for each valid index of image[0] (cols)
                            .anyMatch(col -> image1[row][col] != image2[row][col])
                    )); // if any pixels are different -> true
        // @formatter:on
    }

    /**
     * Compute the skeleton of a boolean image.
     *
     * @param image array containing each pixel's boolean value.
     * @return array containing the boolean value of each pixel of the image after
     * applying the thinning algorithm.
     */
    public static boolean [][] thin(boolean [][] image) {
        assert image != null;
        boolean[][] previous; // define the dimensions of the image
        do {
            previous = Arrays.stream(image).map(boolean[]::clone).toArray(boolean[][]::new);
            for (int row = 0; row < image.length; row++) // store the image's current state
                previous[row] = image[row].clone();
            image = thinningStep(thinningStep(image, 0), 1); // run both thinning steps
        } while (!identical(previous, image)); // repeat if there was a change
        return image;
    }

    /**
     * Internal method used by {@link #thin(boolean[][])}.
     *
     * @param image array containing each pixel's boolean value.
     * @param step  the step to apply, Step 0 or Step 1.
     * @return A new array containing each pixel's value after the step.
     */
    public static boolean [][] thinningStep(boolean [][] image, int step) {
        assert image != null;
        boolean[][] newImage = new boolean[image.length][image[0].length];
        IntStream
            .range(0, image.length) // for each valid index of image (rows)
            .forEach(row -> IntStream
                .range(0, image[0].length) // for each valid index of image[0] (cols)
                .filter(col -> image[row][col]) // if the pixel is white it doesn't change
                .forEach(col -> { // set newImage[row][col] = to the new pixels after seeing if they should stay
                        boolean[] neighbours = getNeighbours(image, row, col); // simplifies the logic below
                        // @formatter:off
                        // if any condition is true the pixel stays true (a boolean simplification of the given conditions)
                        newImage[row][col] = neighbours == null
                            || blackNeighbours(neighbours) <= 1
                            || blackNeighbours(neighbours) >= 7
                            || transitions(neighbours) != 1
                            || (step != 0
                                    || (IntStream.of(2, 4).allMatch(i -> neighbours[i])
                                        && IntStream.of(0, 6).anyMatch(i -> neighbours[i]))
                                ) && (step != 1
                                    || (IntStream.of(0, 6).allMatch(i -> neighbours[i])
                                        && IntStream.of(2, 4).anyMatch(i -> neighbours[i])));
                        // @formatter:on
                    }
                ));
        return newImage;
    }

    /**
     * Computes all pixels that are connected to the pixel at coordinate
     * <code>(row, col)</code> and within the given distance of the pixel.
     *
     * @param image    array containing each pixel's boolean value.
     * @param row      the first coordinate of the pixel of interest.
     * @param col      the second coordinate of the pixel of interest.
     * @param distance the maximum distance at which a pixel is considered.
     * @return An array where <code>true</code> means that the pixel is within
     * <code>distance</code> and connected to the pixel at
     * <code>(row, col)</code>.
     */
    public static boolean [][] connectedPixels(boolean[][] image, int row, int col, int distance) {
        assert image != null;
        var squareSideLength = (2 * distance) + 1;
        // create a square clone of a subset of the image centered on the pixel and squareSideLength wide
        var clone = subClone(
            image,
            row - distance, // new center of sub image
            col - distance,
            squareSideLength
        );
        // set an empty array with the same center pixel as in clone
        boolean[][] relevant = new boolean[squareSideLength][squareSideLength];
        relevant[distance][distance] = image[row][col];
        boolean[][] previousArray; // declared outside of loop scope so that it can be used in the check
        do {
            previousArray = subClone(relevant, 0, 0, squareSideLength); // clone relevant to remember it
            IntStream
                .range(0, squareSideLength)
                .forEach(rowIndex -> IntStream
                    .range(0, squareSideLength)
                    .forEach(colIndex -> // make every pixel "infect" its true neighbours if it is true
                        spreadCentrePixel(clone, relevant, rowIndex, colIndex)
                    ));
        } while (!identical(previousArray, relevant)); // repeat if there was a change
        return relevant;
    }

    static boolean [][] subClone(boolean [][] image, int topLeftRow, int topLeftCol, int width) {
        assert image != null;
        final boolean[][] clone = new boolean[width][width];
        IntStream
            .range(Math.max(0, topLeftRow), Math.min(image.length, topLeftRow + width)) // for the overlapping rows
            .forEach(row ->
                System.arraycopy( // copy the overlapping columns
                    image[row],
                    Math.max(0, topLeftCol),
                    clone[row - topLeftRow],
                    Math.abs(Math.min(0, topLeftCol)),
                    Math.min(image[row].length, topLeftCol + width) - Math.max(0, topLeftCol)
                ));
        return clone;
    }


    static void spreadCentrePixel(boolean [][] image, boolean[][] relevant, int rowIndex, int colIndex) {
        assert image != null;
        assert relevant != null;
        // check if pixel is in the image bounds
        if (rowIndex < 0
            || rowIndex >= image.length
            || colIndex < 0
            || colIndex >= image[0].length
        ) return;

        // if the pixel is false it doesn't spread
        if (!relevant[rowIndex][colIndex]) return;

        // @formatter:off
        // check which of the sides of the 3x3 around the pixel are inbounds
        var topRowInImage      = (rowIndex > 0                    );
        var rightColumnInImage = (colIndex < (image[0].length - 1));
        var bottomRowInImage   = (rowIndex < (image.length - 1)   );
        var leftColumnInImage  = (colIndex > 0                    );

        // for each pixel: if it's inbounds and true in the original image, make it true
        // if statements instead of ternary operators (as in getNeighbours) because
        // the subsetClone indexes also have to be inbounds
        if (topRowInImage                           ) relevant[rowIndex - 1][colIndex    ] = image[rowIndex - 1][colIndex    ];
        if (topRowInImage      && rightColumnInImage) relevant[rowIndex - 1][colIndex + 1] = image[rowIndex - 1][colIndex + 1];
        if (rightColumnInImage                      ) relevant[rowIndex    ][colIndex + 1] = image[rowIndex    ][colIndex + 1];
        if (rightColumnInImage && bottomRowInImage  ) relevant[rowIndex + 1][colIndex + 1] = image[rowIndex + 1][colIndex + 1];
        if (bottomRowInImage                        ) relevant[rowIndex + 1][colIndex    ] = image[rowIndex + 1][colIndex    ];
        if (bottomRowInImage   && leftColumnInImage ) relevant[rowIndex + 1][colIndex - 1] = image[rowIndex + 1][colIndex - 1];
        if (leftColumnInImage                       ) relevant[rowIndex    ][colIndex - 1] = image[rowIndex    ][colIndex - 1];
        if (leftColumnInImage  && topRowInImage     ) relevant[rowIndex - 1][colIndex - 1] = image[rowIndex - 1][colIndex - 1];
        // @formatter:on
    }

    /**
     * Computes the slope of a minutia using linear regression.
     *
     * @param connectedPixels the result of
     *                        {@link #connectedPixels(boolean[][], int, int, int)}.
     * @param row             the row of the minutia.
     * @param col             the col of the minutia.
     * @return the slope.
     */
    public static double computeSlope(boolean [][] connectedPixels, int row, int col) {
        assert connectedPixels != null;
        var xValues = new ArrayList<Integer>();
        var yValues = new ArrayList<Integer>();

        IntStream
            .range(0, connectedPixels.length)
            .forEach(rowIndex -> IntStream
                .range(0, connectedPixels[0].length)
                // only if the pixel is true
                .filter(colIndex -> connectedPixels[rowIndex][colIndex])
                // and only if the pixel isn't the minutia in question
                .filter(colIndex -> !(rowIndex == row && colIndex == col))
                .forEach(colIndex -> { // add an adjusted value to the list
                    xValues.add(colIndex - col);
                    yValues.add(row - rowIndex);
                }));

        // sum of x times y
        double xySum = IntStream
            .range(0, xValues.size())
            .mapToDouble(i -> xValues.get(i) * yValues.get(i))
            .sum();

        // the sum of the squares of x and y
        double xSquared = xValues.stream()
            .mapToDouble(i -> i * i)
            .sum();
        double ySquared = yValues.stream()
            .mapToDouble(i -> i * i)
            .sum();

        // @formatter:off
        if (xSquared == 0       ) return Double.POSITIVE_INFINITY; // if vertical return infinity
        if (xSquared >= ySquared) return xySum / xSquared;
        else                      return ySquared / xySum;
        // @formatter:on
    }

    /**
     * Computes the orientation of a minutia in radians.
     *
     * @param connectedPixels the result of
     *                        {@link #connectedPixels(boolean[][], int, int, int)}.
     * @param row             the row of the minutia.
     * @param col             the col of the minutia.
     * @param slope           the slope as returned by
     *                        {@link #computeSlope(boolean[][], int, int)}.
     * @return the orientation of the minutia in radians.
     */
    public static double computeAngle(boolean [][] connectedPixels, int row, int col, double slope) {
        assert connectedPixels != null;
        // atomics because they're used in a lambda expression below
        var pixelsAbove = new AtomicInteger();
        var pixelsUnder = new AtomicInteger();
        IntStream
            .range(0, connectedPixels.length)
            .forEach(rowIndex -> IntStream
                .range(0, connectedPixels[0].length)
                .filter(colIndex -> connectedPixels[rowIndex][colIndex]) // only if the pixel is true
                .filter(colIndex -> !(rowIndex == row && colIndex == col)) // and it's not the origin
                .forEach(colIndex -> {
                    int x = colIndex - col; // make its coordinates relative to the new origin
                    int y = row - rowIndex;
                    if (y >= (-1 / slope) * x)
                        pixelsAbove.getAndIncrement(); // if it's above the normal increment pixelsAbove
                    else
                        pixelsUnder.getAndIncrement(); // if not increment pixels below
                }));

        if (slope == Double.POSITIVE_INFINITY) // if the line is vertical
            return (pixelsAbove.get() > pixelsUnder.get() ? Math.PI : -Math.PI) / 2; // return either up or down

        double angle = Math.atan(slope);
        if ((angle > 0 && pixelsUnder.get() > pixelsAbove.get()) // if it's going up and there are more under the line than not
            || (angle < 0 && pixelsUnder.get() < pixelsAbove.get()) // or going down and more over than under
        ) angle += Math.PI; // flip the angle
        return angle;
    }

    /**
     * Computes the orientation of the minutia that the coordinate <code>(row,
     * col)</code>.
     *
     * @param image    array containing each pixel's boolean value.
     * @param row      the first coordinate of the pixel of interest.
     * @param col      the second coordinate of the pixel of interest.
     * @param distance the distance to be considered in each direction to compute
     *                 the orientation.
     * @return The orientation in degrees.
     */
    public static int computeOrientation(boolean[][] image, int row, int col, int distance) {
        assert image != null;
        var connectedPixels = connectedPixels(image, row, col, distance);
        var slope = computeSlope(connectedPixels, distance, distance);
        var angle = computeAngle(connectedPixels, distance, distance, slope);
        var angleDegrees = (int) Math.round(Math.toDegrees(angle));
        return angleDegrees < 0 ? angleDegrees + 360 : angleDegrees;
    }

    /**
     * Extracts the minutiae from a thinned image.
     *
     * @param image array containing each pixel's boolean value.
     * @return The list of all minutiae. A minutia is represented by an array where
     * the first element is the row, the second is column, and the third is
     * the angle in degrees.
     * @see #thin(boolean[][])
     */
    public static List<int[]> extract(boolean[][] image) {
        assert image != null;
        var minutiaes = new ArrayList<int[]>();
        IntStream
            .range(1, image.length - 1) // for each pixel excluding the outer edge
            .forEach(row -> IntStream
                .range(1, image[0].length - 1)
                .filter(col -> image[row][col]) // if it's part of the fingerprint (i.e. true)
                .filter(col -> { // if it's a minutia
                    var neighbors = getNeighbours(image, row, col);
                    assert neighbors != null;
                    var transitions = transitions(neighbors);
                    return transitions == 3 || transitions == 1;
                })
                .forEach(col -> // add it to the list
                    minutiaes.add(new int[]{
                        row,
                        col,
                        computeOrientation(image, row, col, ORIENTATION_DISTANCE)
                    })
                ));
        return minutiaes;
    }

    /**
     * Applies the specified rotation to the minutia.
     *
     * @param minutia   the original minutia.
     * @param centerRow the row of the center of rotation.
     * @param centerCol the col of the center of rotation.
     * @param rotation  the rotation in degrees.
     * @return the minutia rotated around the given center.
     */
    public static int [] applyRotation(int [] minutia, int centerRow, int centerCol, int rotation) {
        assert minutia != null;
        // center on new origin
        int x = minutia[1] - centerCol;
        int y = centerRow - minutia[0];
        // convert to radians, then compute sin/cos
        double rotationRad = Math.toRadians(rotation);
        double sinRot = Math.sin(rotationRad);
        double cosRot = Math.cos(rotationRad);
        // formula
        int newRow = (int) Math.round(centerRow - (x * sinRot + y * cosRot));
        int newCol = (int) Math.round(centerCol + (x * cosRot - y * sinRot));
        int newOrientation = (minutia[2] + rotation) % 360;
        return new int[]{newRow, newCol, newOrientation};
    }

    /**
     * Applies the specified translation to the minutia.
     *
     * @param minutia        the original minutia.
     * @param rowTranslation the translation along the rows.
     * @param colTranslation the translation along the columns.
     * @return the translated minutia.
     */
    public static int [] applyTranslation(int [] minutia, int rowTranslation, int colTranslation) {
        assert minutia != null;
        int newRow = minutia[0] - rowTranslation;
        int newCol = minutia[1] - colTranslation;
        return new int[]{newRow, newCol, minutia[2]};
    }

    /**
     * Computes the row, column, and angle after applying a transformation
     * (translation and rotation).
     *
     * @param minutia        the original minutia.
     * @param centerCol      the column around which the point is rotated.
     * @param centerRow      the row around which the point is rotated.
     * @param rowTranslation the vertical translation.
     * @param colTranslation the horizontal translation.
     * @param rotation       the rotation.
     * @return the transformed minutia.
     */
    public static int [] applyTransformation(int[] minutia,
                                                      int centerRow,
                                                      int centerCol,
                                                      int rowTranslation,
                                                      int colTranslation,
                                                      int rotation) {
        assert minutia != null;
        return applyTranslation(
            applyRotation(minutia, centerRow, centerCol, rotation),
            rowTranslation,
            colTranslation
        );
    }

    /**
     * Computes the row, column, and angle after applying a transformation
     * (translation and rotation) for each minutia in the given list.
     *
     * @param minutiae       the list of minutiae.
     * @param centerCol      the column around which the point is rotated.
     * @param centerRow      the row around which the point is rotated.
     * @param rowTranslation the vertical translation.
     * @param colTranslation the horizontal translation.
     * @param rotation       the rotation.
     * @return the list of transformed minutiae.
     */
    public static List<int[]> applyTransformation(List<int[]> minutiae,
                                                  int centerRow,
                                                  int centerCol,
                                                  int rowTranslation,
                                                  int colTranslation,
                                                  int rotation) {
        assert minutiae != null;
        return minutiae.stream()
            .map(i -> applyTransformation(i, centerRow, centerCol, rowTranslation, colTranslation, rotation))
            .collect(Collectors.toList());
    }

    /**
     * Counts the number of overlapping minutiae.
     *
     * @param minutiae1      the first set of minutiae.
     * @param minutiae2      the second set of minutiae.
     * @param maxDistance    the maximum distance between two minutiae to consider
     *                       them as overlapping.
     * @param maxOrientation the maximum difference of orientation between two
     *                       minutiae to consider them as overlapping.
     * @return the number of overlapping minutiae.
     */
    public static int matchingMinutiaeCount(List<int[]> minutiae1,
                                            List<int[]> minutiae2,
                                            int maxDistance,
                                            int maxOrientation) {
        assert minutiae1 != null;
        assert minutiae2 != null;
        return (int) minutiae1.stream() // for each minutia in minutiae1
            .filter(minutia1 -> minutiae2.stream()
                .anyMatch(minutia2 -> { // keep it if there is any in minutiae2 that is true below
                    var a = minutia1[0] - minutia2[0];
                    var b = minutia1[1] - minutia2[1];
                    return Math.sqrt(a * a + b * b) <= maxDistance // Pythagoras
                        && Math.abs(minutia1[2] - minutia2[2]) <= maxOrientation;
                })
            ).count();
    }

    /**
     * Compares the minutiae from two fingerprints.
     *
     * @param minutiae1 the list of minutiae of the first fingerprint.
     * @param minutiae2 the list of minutiae of the second fingerprint.
     * @return Returns <code>true</code> if they match and <code>false</code>
     * otherwise.
     */
    public static boolean match(List<int[]> minutiae1, List<int[]> minutiae2) {
        assert minutiae1 != null;
        assert minutiae2 != null;
        return minutiae1.stream()
            .anyMatch(min1 -> minutiae2.stream().parallel() // is there any in minutia2 where
                .anyMatch(min2 -> IntStream // in the angle_offset range
                    .rangeClosed(
                        (min2[2] - min1[2]) - MATCH_ANGLE_OFFSET,
                        (min2[2] - min1[2]) + MATCH_ANGLE_OFFSET
                    ).parallel()
                    .anyMatch(rotation -> // is any transformation similar by >= threshold amount
                        FOUND_THRESHOLD <=
                            matchingMinutiaeCount(
                                minutiae1,
                                applyTransformation(
                                    minutiae2,
                                    min1[0],
                                    min1[1],
                                    min2[0] - min1[0],
                                    min2[1] - min1[1],
                                    rotation
                                ),
                                DISTANCE_THRESHOLD,
                                ORIENTATION_THRESHOLD
                            ))));
    }
}
