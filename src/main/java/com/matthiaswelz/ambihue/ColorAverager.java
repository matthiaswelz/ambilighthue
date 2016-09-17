package com.matthiaswelz.ambihue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.stackoverflow.finnw.CIELab;

public class ColorAverager {
	private final List<Pair<Color, Double>> colors;

	public ColorAverager() {
		this.colors = new ArrayList<>();
	}

	public void addColor(Color color, double weight) {
		assert color != null;
		assert weight > 0;

		this.colors.add(new ImmutablePair<Color, Double>(color, weight));
	}

	public Color calculateResult() {
		float totalL = 0;
		float totalA = 0;
		float totalB = 0;
		float totalWeight = 0;

		for (Pair<Color, Double> pair : colors) {
			Color color = pair.getKey();
			double weight = pair.getValue();

			float[] lab = CIELab.getInstance().fromRGB(new float[] { (color.getR() & 0xFF) / 255.0f, (color.getG() & 0xFF) / 255.0f, (color.getB() & 0xFF) / 255.0f});

			totalL += lab[0] * weight;
			totalA += lab[1] * weight;
			totalB += lab[2] * weight;
			totalWeight += weight;
		}

		float l = totalL / totalWeight;
		float a = totalA / totalWeight;
		float b = totalB / totalWeight;

		float[] rgb = CIELab.getInstance().toRGB(new float[] { l, a, b });
		return new Color((byte)(int) (rgb[0] * 255 + 0.5f), (byte)(int) (rgb[1] * 255 + 0.5f), (byte)(int) (rgb[2] * 255 + 0.5f));
	}
}
