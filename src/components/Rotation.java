package components;
import java.util.ArrayList;


public class Rotation {

	String name;
	Service service;
	ArrayList<Block> blockList;
	
	public Rotation(String name, Service service){
		this.name = name;
		this.service = service;
	}
	
	public String getName(){
		return name;
	}
	
	public Service getService(){
		return service;
	}

	public void addBlockList(ArrayList<Block> blockList) {
		this.blockList = blockList;
	}
	
	public ArrayList<Block> getBlockList(){
		return this.blockList;
	}
}
