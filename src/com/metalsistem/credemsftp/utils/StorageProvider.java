package com.metalsistem.credemsftp.utils;

import org.compiere.model.IArchiveStore;
import org.compiere.model.MArchive;
import org.compiere.model.MStorageProvider;

public class StorageProvider implements IArchiveStore{

	@Override
	public byte[] loadLOBData(MArchive archive, MStorageProvider prov) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void save(MArchive archive, MStorageProvider prov, byte[] inflatedData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean deleteArchive(MArchive archive, MStorageProvider prov) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPendingFlush() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void flush(MArchive archive, MStorageProvider prov) {
		// TODO Auto-generated method stub
		
	}

}
