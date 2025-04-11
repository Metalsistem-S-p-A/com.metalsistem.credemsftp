package com.metalsistem.credemsftp.utils;

public class UOM {
	private int id;

	public UOM() {
	}

	public UOM(String uom) {
		switch (uom) {
		case "NR":
			this.setId(100);
			break;
		case "Kg":
			this.setId(1000014);
			break;
		default:
			this.setId(100);
		}
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}