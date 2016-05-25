package components;
import java.util.ArrayList;

public class Block {

	private String blockName;
	private int block_min;
	private int block_max;
	private int firm_constraint;
	private String block_type;
	private int durationType; //-1: no preference, 2: 2 weeks, 4: 4 weeks, 8: 8 weeks 
	private Service sv;
	private Rotation rt;
	private ArrayList<Person> people;
	private Block priorBlock;
	private Block postBlock;
	
	public Block(String blockName, int block_min, int block_max,
			int durationType, int firm_constraint, String block_type, Service sv, Rotation rt) {
		// TODO Auto-generated constructor stub
		
		
		this.blockName = blockName;
		this.block_min = block_min;
		this.block_max = block_max;
		this.firm_constraint = firm_constraint;
		this.block_type = block_type;
		this.sv = sv;
		this.rt = rt;
		this.durationType = durationType;
		this.priorBlock = null;
		this.postBlock = null;
		people = new ArrayList<Person>();
	}
	
	public String getBlockName(){
		return blockName;
	}
	
	public int getBlockMin(){
		return block_min;
	}
	
	public int getBlockMax(){
		return block_max;
	}
	
	public int getFirmConstraint(){
		return firm_constraint;
	}
	
	public String getBlockType(){
		return block_type;
	}
	
	public Service getService(){
		return sv;
	}

	public Rotation getRotation(){
		return rt;
	}
	
	public int getDurationType(){
		return durationType;
	}
	
	public void addPerson(Person person){
		if (people.contains(person) && !blockName.equals("X")){
			System.err.println("Person entered twice!");
		}
		people.add(person);
	}
	
	public ArrayList<Person> getPeople(){
		return people;
	}

	public void removePerson(Person person) {
		if (people.contains(person)){
			people.remove(person);
			if (people.contains(person)){
				System.err.println("Person entered twice!");
			}
		} else {
			System.err.println("Person not listed!");
		}
	}
	
	public Block getPriorBlock(){
		return this.priorBlock;
	}
	
	public Block getPostBlock(){
		return this.postBlock;
	}

	public void addPriorBlock(Block priorBlock) {
		this.priorBlock = priorBlock;
	}

	public void addPostBlock(Block postBlock) {
		this.postBlock = postBlock;
	}

}
