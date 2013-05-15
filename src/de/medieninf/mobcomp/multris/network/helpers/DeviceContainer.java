package de.medieninf.mobcomp.multris.network.helpers;


/**
 * 
 * @author marcel bechtold
 * @author tina schedlbauer
 * 
 * This class holds information about Blutooth Devices
 * */
public class DeviceContainer {
	private String name;
	private String address;
	private boolean connected;

	public DeviceContainer(String name, String address) {
		this.name = name;
		this.address = address;
		this.connected = false;
	}

	public String getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DeviceContainer)) {
			return false;
		}
		DeviceContainer device = (DeviceContainer) o;
		return (device.getAddress().equals(address) && device.getName().equals(name));
	}
}