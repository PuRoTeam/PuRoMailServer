package it.prms.amazon.utility;

public enum TableInfo {
	
	TableMetaMail("MetaMail"),
	
	TMetaFolderHash("folder"),
	TMetaUidRange("uid"),
	TMetaHeader("header"),
	TMetaBucket("bucket"),
	TMetaFileName("objectname"),
	TMetaTimestamp("timestamp"),
	TMetaSize("size"),
	TMetaFlags("flags"),
	
	TableFolder("Folder"),
	
	TFolderNameHash("name"),
	TFolderNextUID("nextUID"),
	TFolderParent("parent"),
	TFolderChildren("children"),
    TFolderSelectable("selectable"),
    TFolderLastUpdate("lastupdate"),
	
	TableUser("User"),
	
	TUserEmailHash("email"),
	TUserPassword("password"),
	TUserFirstName("firstname"),
	TUserLastName("lastname"),
	TUserFolder("folder"),
	TUserLastUpdate("lastupdate"),
    //TUserRegion("region"),
	
	BucketNameUSStandard("puro-mail-us-standard"),
	BucketNameEUWest("puro-mail-eu-west");

    
    private final String typeId;

    private TableInfo(String typeId) {
        this.typeId = typeId;
    }

    public String toString() {
        return typeId;
    }
}