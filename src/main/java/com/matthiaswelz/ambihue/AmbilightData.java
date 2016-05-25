package com.matthiaswelz.ambihue;

import org.apache.commons.lang3.builder.ToStringBuilder;

public final class AmbilightData {
	public enum Position {
		Left, Top, Right, Bottom
	}
	
	private Color[][] colors;
	
	public AmbilightData(Color[] left, Color[] top, Color[] right, Color[] bottom) {
		this.colors = new Color[4][];
		
		this.colors[Position.Left.ordinal()] = left;
		this.colors[Position.Top.ordinal()] = top;
		this.colors[Position.Right.ordinal()] = right;
		this.colors[Position.Bottom.ordinal()] = bottom;
	}
	
	public boolean hasPosition(Position position) {		
		Color[] colors = this.getColorsAtPosition(position);
		
		return colors != null && colors.length > 0;
	}
	public int getDimension(Position position) {
		Color[] colors = this.getColorsAtPosition(position);
		
		if (colors == null)
			return 0;
		
		return colors.length;
	}
	public Color getColor(Position position, int index) {
		assert hasPosition(position);
		assert getDimension(position) > index;
		
		Color[] colors = this.getColorsAtPosition(position);
		return colors[index];
	}
	
	private Color[] getColorsAtPosition(Position position) {
		return this.colors[position.ordinal()];
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("left", getColorsAtPosition(Position.Left), true)
				.append("top", getColorsAtPosition(Position.Top), true)
				.append("right", getColorsAtPosition(Position.Right), true)
				.append("bottom", getColorsAtPosition(Position.Bottom), true)
				.build();
	}
}
