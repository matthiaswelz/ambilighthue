package com.matthiaswelz.ambihue;

import org.apache.commons.lang3.builder.ToStringBuilder;

public final class Color {
	private byte r;
	private byte g;
	private byte b;
	
	public Color(byte r, byte g, byte b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public byte getR() {
		return r;
	}
	public byte getG() {
		return g;
	}
	public byte getB() {
		return b;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("r", this.r)
				.append("g", this.g)
				.append("b", this.b)
				.build();
	}
}
