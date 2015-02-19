package it.prms.amazon.utility;

/**
 * Enum
 */
public enum AttributeType{
	
	StringType("string"),

	BynaryType("bynary"),
	
	NumberType("number"),
	
	StringSetType("string_set"),
	
	NumberSetType("number_set");
	
    private final String typeId;

    private AttributeType(String typeId) {
        this.typeId = typeId;
    }

    public String toString() {
        return typeId;
    }
}