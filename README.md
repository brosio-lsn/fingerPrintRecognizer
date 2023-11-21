# FingerPrintRecognizer

## Overview
FingerPrintRecognizer is a Java application designed for fingerprint analysis and recognition. Utilizing advanced image processing techniques, the program can compare fingerprint images to determine if they are from the same individual.

## Key Features
- **Fingerprint Minutiae Extraction**: Implements a crucial step in fingerprint recognition by identifying unique features in fingerprints.
- **Image Skeletonization**: Transforms fingerprint images to a one-pixel thickness format, maintaining the structure while simplifying the image for easier processing.
- **Minutiae Comparison**: Utilizes a simple yet effective approach to compare minutiae points between fingerprints, including their coordinates and orientations.
- **Robust Recognition**: Capable of handling variations in fingerprint orientation and positioning, ensuring accurate identification.
- **Java Helper Tools**: Includes utilities for image manipulation and display, aiding in the development and debugging process.

## Technical Implementation
- **Squelettisation**: Implements functions necessary for image binary skeletonization.
- **Minutiae Coordinates and Orientation Extraction**: Develops functions to extract and calculate the orientation of minutiae.
- **Comparison Algorithm**: Implements functions to determine if two sets of minutiae are from the same finger.

## Usage
The provided `Main.java` file serves as the starting point for testing and verifying the program's functionalities. Users can input fingerprint images, and the application will process and compare them.

## Installation and Running
Instructions for setting up and running the FingerPrintRecognizer are included in the project's documentation.

## Acknowledgments
- EPFL for providing the educational framework and project guidance.
