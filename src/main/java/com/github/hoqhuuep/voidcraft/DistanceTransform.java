package com.github.hoqhuuep.voidcraft;

/**
 * An implementation of the Distance Transform algorithm described on this
 * paper: http://cs.brown.edu/~pff/papers/dt-final.pdf
 */
public class DistanceTransform {
	private static final double INF = 1e20;

	public static double[][] distanceTransform(boolean[][] image, boolean on) {
		int numColumns = image[0].length;
		int numRows = image.length;
		double[][] result = new double[numRows][numColumns];

		// Transform columns
		for (int column = 0; column < numColumns; column++) {
			double[] tempColumn = new double[numRows];
			for (int row = 0; row < numRows; row++) {
				tempColumn[row] = image[row][column] == on ? 0 : INF;
			}
			tempColumn = quadranceTransform(tempColumn);
			for (int row = 0; row < numRows; row++) {
				result[row][column] = tempColumn[row];
			}
		}

		// Transform rows
		for (int row = 0; row < numRows; row++) {
			result[row] = quadranceTransform(result[row]);
			for (int column = 0; column < numColumns; column++) {
				result[row][column] = Math.sqrt(result[row][column]);
			}
		}

		return result;
	}

	private static double[] quadranceTransform(double[] function) {
		int numSamples = function.length;
		int segment = 0;
		int[] lowerEnvelope = new int[numSamples];
		lowerEnvelope[segment] = 0;
		double[] boundaries = new double[numSamples + 1];
		boundaries[segment] = -INF;

		for (int sample = 1; sample < numSamples; ++sample) {
			double intersection = calculateIntersection(function, sample, lowerEnvelope[segment]);
			while (intersection <= boundaries[segment]) {
				--segment;
				intersection = calculateIntersection(function, sample, lowerEnvelope[segment]);
			}
			segment++;
			lowerEnvelope[segment] = sample;
			boundaries[segment] = intersection;
		}
		boundaries[segment + 1] = INF;

		double[] result = new double[numSamples];
		segment = 0;
		for (int sample = 0; sample < numSamples; ++sample) {
			while (boundaries[segment + 1] < sample) {
				segment++;
			}
			result[sample] = square(sample - lowerEnvelope[segment]) + function[lowerEnvelope[segment]];
		}

		return result;
	}

	private static double calculateIntersection(double[] function, int s1, int s2) {
		return ((function[s1] + square(s1)) - (function[s2] + square(s2))) / (2 * s1 - 2 * s2);
	}

	private static double square(double x) {
		return x * x;
	}
}
