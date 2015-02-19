package it.prms.amazon.update.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.amazonaws.services.dynamodb.model.AttributeAction;

public class UpdateAttributeList
{
	HashMap<String, UpdateAttribute> hashmap;
	
	public UpdateAttributeList()
	{
		hashmap = new HashMap<String, UpdateAttribute>();				
	}
	
	public void put(String key, UpdateAttribute newAttribute)
	{
		hashmap.put(key, newAttribute);
	}
	
	public void remove(String key)
	{
		hashmap.remove(key);
	}
	
	public UpdateAttribute getAttributeObject(String key)
	{
		return hashmap.get(key);
	}
	
	public ArrayList<String> getAttributeNames()
	{
		Set<String> set = hashmap.keySet();
		
		Iterator<String> iterator = set.iterator();
		ArrayList<String> keys = new ArrayList<String>();
		
		while(iterator.hasNext())		
			keys.add(iterator.next());
		
		return keys;
	}
	
}
