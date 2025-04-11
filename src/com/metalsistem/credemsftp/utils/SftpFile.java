package com.metalsistem.credemsftp.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.schmizz.sshj.xfer.InMemorySourceFile;

public class SftpFile extends InMemorySourceFile {

	private String name;
	private byte[] data;

	public SftpFile(String name, byte[] data) {
		this.name = name;
		this.data = data;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(data);
	}

	@Override
	public long getLength() {
		return data.length;
	}

	@Override
	public String getName() {
		return name;
	}

}
