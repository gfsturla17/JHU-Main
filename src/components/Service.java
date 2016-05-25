package components;
import java.util.ArrayList;


public class Service {

	private String name;
	
	private ArrayList<Rotation> rotations;

	public Service(String name){
		rotations = new ArrayList<Rotation>();
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}

	public void addRotation(Rotation rt) {
		rotations.add(rt);		
	}
	
	public ArrayList<Rotation> getRotations(){
		return rotations;
	}
}
