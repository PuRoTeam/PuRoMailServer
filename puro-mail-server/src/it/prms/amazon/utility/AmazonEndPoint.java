package it.prms.amazon.utility;

public enum AmazonEndPoint {
	
	EC2USEastNorthernVirginia("ec2.us-east-1.amazonaws.com"),
	EC2USWestOregon("ec2.us-west-2.amazonaws.com"),
	EC2USWestNorthernCalifornia("ec2.us-west-1.amazonaws.com"),
	EC2EUIreland("ec2.eu-west-1.amazonaws.com"),
	EC2AsiaPacificSingapore("ec2.ap-southeast-1.amazonaws.com"),
	EC2AsiaPacificSydney("ec2.ap-southeast-2.amazonaws.com"),
	EC2AsiaPacificTokyo("ec2.ap-northeast-1.amazonaws.com"),
	EC2SouthAmericaSaoPaulo("ec2.sa-east-1.amazonaws.com"),
	
	DDBUSEastNorthernVirginia("dynamodb.us-east-1.amazonaws.com"),
	DDBUSWestOregon("dynamodb.us-west-2.amazonaws.com"),
	DDBUSWestNorthernCalifornia("dynamodb.us-west-1.amazonaws.com"),
	DDBEUIreland("dynamodb.eu-west-1.amazonaws.com"),
	DDBAsiaPacificSingapore("dynamodb.ap-southeast-1.amazonaws.com"),
	DDBAsiaPacificSydney("dynamodb.ap-southeast-2.amazonaws.com"),
	DDBAsiaPacificTokyo("dynamodb.ap-northeast-1.amazonaws.com"),
	DDBSouthAmericaSaoPaulo("dynamodb.sa-east-1.amazonaws.com"),

	S3USEastNorthernVirginia("s3-us-east-1.amazonaws.com"),
	S3USWestOregon("s3-us-west-2.amazonaws.com"),
	S3USWestNorthernCalifornia("S3-us-west-1.amazonaws.com"),
	S3EUIreland("s3-eu-west-1.amazonaws.com"),
	S3AsiaPacificSingapore("s3-ap-southeast-1.amazonaws.com"),
	S3AsiaPacificSydney("s3-ap-southeast-2.amazonaws.com"),
	S3AsiaPacificTokyo("s3-ap-northeast-1.amazonaws.com"),
	S3SouthAmericaSaoPaulo("s3-sa-east-1.amazonaws.com");
	
	private final String endpoint;
	
	AmazonEndPoint(String endpoint){
		this.endpoint = endpoint;
	}
	
	public String toString() {
        return this.endpoint;
    }
}
