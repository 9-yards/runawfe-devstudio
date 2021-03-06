package ru.runa.gpd.jseditor;

/**
 * The model for the JavaScript variable.
 * 
 * @author Naoki Takezoe
 */
public class JavaScriptVariable implements JavaScriptElement {
	
	private String name;
	private int offset;
	
	public JavaScriptVariable(String name, int offset){
		this.name = name;
		this.offset = offset;
	}
	
	public String getName() {
		return name;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public String toString(){
		return getName();
	}
}
