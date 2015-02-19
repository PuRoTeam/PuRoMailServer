package it.prms.amazon.update.attribute;

import it.prms.amazon.utility.AttributeType;

import java.util.ArrayList;

import com.amazonaws.services.dynamodb.model.AttributeAction;

public class MultiUpdateAttribute extends UpdateAttribute
{
	ArrayList<String> values;
	AttributeType type;
	AttributeAction attributeAction;
	
	public MultiUpdateAttribute(ArrayList<String> values, AttributeAction attributeAction) //SS supporta ADD, PUT e DELETE
	{
		this.values = values;
		type = AttributeType.StringSetType;
		this.attributeAction = attributeAction;
	}
	
	public MultiUpdateAttribute(long[] values, AttributeAction attributeAction) //NS supporta ADD, PUT e DELETE
	{
		for(int i = 0; i < values.length; i++)
			this.values.add("" + values[i]); //converte long in stringa

		type = AttributeType.NumberSetType;
		this.attributeAction = attributeAction;
	}		
			
	public ArrayList<String> getValues()
	{
		return values;
	}
	
	public String[] getArrayOfValues()
	{
		String[] valuesArray = new String[values.size()];		
		values.toArray(valuesArray);
		
		return valuesArray;
	}
	
	public AttributeType getType()
	{
		return type;
	}
	
	public AttributeAction getAttributeAction()
	{
		return attributeAction;
	}
}
