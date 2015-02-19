package it.prms.amazon.update.attribute;

import java.util.ArrayList;

import it.prms.amazon.utility.AttributeType;

import com.amazonaws.services.dynamodb.model.AttributeAction;

public class SingleUpdateAttribute extends UpdateAttribute
{
	String value;
	AttributeType type; //NumberType o StringType
	AttributeAction attributeAction; ///ADD, PUT, DELETE
		
	public SingleUpdateAttribute(String value, AttributeAction attributeAction) throws WrongActionException //S supporta solo PUT
	{
		if(attributeAction.equals(AttributeAction.ADD))
			throw new WrongActionException("STRING Attribute doesn't support ADD Attribute Action");
		else if(attributeAction.equals(AttributeAction.DELETE))
			throw new WrongActionException("STRING Attribute doesn't support DELETE Attribute Action");
		
		this.value = value;
		type = AttributeType.StringType;
		this.attributeAction = attributeAction;
	}
	
	public SingleUpdateAttribute(long value, AttributeAction attributeAction) throws WrongActionException  //N supporta ADD (somma numerica) e PUT
	{
		if(attributeAction.equals(AttributeAction.DELETE))
			throw new WrongActionException("NUMBER Attribute doesn't support DELETE Attribute Action");
		
		this.value = "" + value; //converte long in stringa			
		type = AttributeType.NumberType;
		this.attributeAction = attributeAction;
	}
	
	public String getValue()
	{
		return value;
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
