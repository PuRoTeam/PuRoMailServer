package it.prms.amazon.services;

import it.prms.amazon.utility.AmazonEndPoint;
import it.prms.amazon.utility.MetaMail;
import it.prms.amazon.utility.TableInfo;
import it.prms.greenmail.foedus.util.StringBufferResource;
import it.prms.greenmail.util.GreenMailUtil;
import it.prms.greenmail.util.InternetPrintWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/*
 * Amazon S3 is a simple key, value store designed to store as many objects as you want. You store these objects in one or more buckets. An object consists of the following:

    Key—The name that you assign to an object. You use the object key to retrieve the object.
    For more information, see Object Key and Metadata

    Version ID—Within a bucket, a key and version ID uniquely identify an object.
    The version ID is a string that Amazon S3 generates when you add an object to a bucket. For more information, see Object Versioning.

    Value—The content that you are storing.
    An object value can be any sequence of bytes. Objects can range from zero to 5 TB in size. For more information, see Uploading Objects.

    Metadata—A set of name-value pairs with which you can store information regarding the object.
    You can assign metadata, referred to as user-defined metadata, to your objects in Amazon S3. Amazon S3 also assigns system-metadata to these objects, which it uses for managing objects. For more information, see Object Key and Metadata.

    Subresources—Amazon S3 uses the subresource mechanism to store object-specific additional information.
    Because subresources are subordinates to objects, they are always associated with some other entity such as an object or a bucket. For more information, see Object Subresources.

    Access Control Information—You can controls access to the objects you store in Amazon S3.
    
    Object Expiration quando si vuole che un file venga eliminato dopo un certo periodo di tempo.
 * */

public class S3 {
	
	private AmazonS3Client s3;
	private AmazonEndPoint currentEndPoint;
	
	/****************************************************Costruttori**************************************************/
		
	public S3() throws FileNotFoundException, IllegalArgumentException, IOException {
		this("", null);
    }
	
	public S3(AmazonEndPoint endpoint) throws FileNotFoundException, IllegalArgumentException, IOException {
		this("", endpoint);
    }
	
	public S3(String credentials_path) throws FileNotFoundException, IllegalArgumentException, IOException {
		this(credentials_path, null);
    }
	
	/**
	 * Costruttore S3
	 * @param credentials_path, percorso assoluto del file contenente le credenziali di accesso agli AWS
	 * @param endpoint, specifica la regione in cui operare
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public S3(String credentials_path, AmazonEndPoint endpoint) throws FileNotFoundException, IllegalArgumentException, IOException {	
		
		AWSCredentials credentials = null;
		
		if(credentials_path.equals(""))
			credentials = new PropertiesCredentials(S3.class.getResourceAsStream("AwsCredentials.properties"));
		else
			credentials = new PropertiesCredentials(new File(credentials_path));
					
        s3 = new AmazonS3Client(credentials);
        
        this.setCurrentEndPoint(endpoint);
	}	
	
	
	/****************************************************Costruttori**************************************************/
	
	public void setCurrentEndPoint(AmazonEndPoint currentEndPoint) throws IllegalArgumentException {
		
		if(currentEndPoint != null) {
			s3.setEndpoint(currentEndPoint.toString());
			this.currentEndPoint = currentEndPoint;
		}
		else 
			this.currentEndPoint = currentEndPoint;
	}
	
	public AmazonEndPoint getCurrentEndPoint() {
		return currentEndPoint;
	}

	/**
	 * Tenta la creazione di un bucket nell'endpoint corrente
	 * @param bucketName
	 * @return true se il bucket è stato creato con successo, false se il bucket esisteva già
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */	
	public boolean createNewBucket(String bucketName) throws AmazonServiceException, AmazonClientException {
		if(s3.doesBucketExist(bucketName))
			return false;		
		else {	
			s3.createBucket(bucketName);
			return true;
		}
	}
    
	/**
	 * Elenca tutti i nomi dei bucket, nell'endpoint corrente
	 * @return lista dei nomi dei bucket
	 */
	public ArrayList<String> getBucketsName(){		
		List<Bucket> bucketList = s3.listBuckets();		
		Iterator<Bucket> iterator = bucketList.iterator();
		
		ArrayList<String> bucketsName = new ArrayList<String>();
		
		while(iterator.hasNext())
			bucketsName.add(iterator.next().getName());
		
		return bucketsName;
	}

	/**
	 * 
	 * @param bucketName
	 * @param key
	 * @param file
	 * @return
	 * @throws AmazonClientException
	 * @throws AmazonServiceException
	 */
	public PutObjectResult uploadingFileToBucket(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException{
		PutObjectResult result = s3.putObject(new PutObjectRequest(bucketName, key, file));		
		return result;
	}
	
	/**
	 * Permette il salvataggio di un intero messaggio di posta in uno specifico bucket di S3
	 * @param message
	 * @param bucketName, nome del bucket
	 * @param key, nome del file
	 * @return il risultato dell'operazione
	 * @throws AmazonClientException
	 * @throws IOException
	 * @throws MessagingException
	 */
	public PutObjectResult uploadFileToBucket(MimeMessage message, String bucketName, String key) 
			throws AmazonClientException, IOException, MessagingException{
		
		File file = new File("tempfile");
    	OutputStream output = new FileOutputStream(file);
    	
    	message.writeTo(output);
		PutObjectResult result = uploadingFileToBucket(bucketName, key, file);
		file.delete();
		
		return result;
	}

    /* IMPORTANTE
     * Download an object - When you download an object, you get all of
     * the object's metadata and a stream from which to read the contents.
     * It's important to read the contents of the stream as quickly as
     * possibly since the data is streamed directly from Amazon S3 and your
     * network connection will remain open until you read all the data or
     * close the input stream.
     *
     * GetObjectRequest also supports several other options, including
     * conditional downloading of objects based on modification times,
     * ETags, and selectively downloading a range of an object.
     */
	/**
	 * Permette il recupero di un file da un bucket di S3
	 * @param bucketName, nome bucket
	 * @param key, nome file
	 * @return il messaggio di posta, creato a partire dal file
	 * @throws MessagingException
	 * @throws AmazonClientException
	 * @throws AmazonServiceException
	 * @throws IOException
	 */
	public MimeMessage downloadObjectFromBucket(String bucketName, String key) 
			throws MessagingException, AmazonClientException, AmazonServiceException, IOException{
	    
		S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));

		return readDataFromTextInputStream(object.getObjectContent());
	}
	
	/**
	 * Legge dati da un stream di input e li aggrega formando un MimeMessage
	 * @param input
	 * @return il mime message letto
	 * @throws IOException
	 * @throws MessagingException
	 */
	public MimeMessage readDataFromTextInputStream(InputStream input) throws IOException, MessagingException {
        
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	StringBufferResource content = new StringBufferResource();
        Writer data = content.getWriter();
        PrintWriter dataWriter = new InternetPrintWriter(data);

        while (true) {
        	String line = reader.readLine();
            if (line == null)
            	break;
            dataWriter.println(line);
        }
        
        dataWriter.close();
        
		return GreenMailUtil.newMimeMessage(content.getAsString());
    }

	/**
	 * S3ObjectSummary NON contiene tutte le informazioni sul singolo oggetto, ma solo alcune
	 * 
	 * @param bucketName
	 * @return
	 * @throws AmazonClientException
	 * @throws AmazonServiceException
	 */
	public ArrayList<S3ObjectSummary> listObjectsInBuckets(String bucketName) throws AmazonClientException, AmazonServiceException{
	    ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName));
	    
	    ArrayList<S3ObjectSummary> completeList = new ArrayList<S3ObjectSummary>(); //Lista finale
	    
	    List<S3ObjectSummary> tempList = objectListing.getObjectSummaries();
	    Iterator<S3ObjectSummary> tempIterator = tempList.iterator();
	      
	   while(tempIterator.hasNext())
		   completeList.add(tempIterator.next());
		   
	   while(objectListing.isTruncated()){ //Non ho recuperato tutta la lista di oggetti (nel caso di lista molto numerosa)
		   objectListing = s3.listNextBatchOfObjects(objectListing); //Riassegno allo stesso oggetto, per andare alla pagina successiva dei risultati
		   
		   tempList = objectListing.getObjectSummaries();
		   tempIterator = tempList.iterator();
		   
	    	while(tempIterator.hasNext())
	    		completeList.add(tempIterator.next());
	   }  
	    
	   return completeList;
	}
    
	/**
	 * Cancella definitivamente un oggetto (a meno che non sia attivato il versioning)
	 * 
	 * @param bucketName
	 * @param key
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public void deleteObjectFromBuckets(String bucketName, String key) throws AmazonServiceException, AmazonClientException {
		s3.deleteObject(bucketName, key); //If attempting to delete an object that does not exist, Amazon S3 returns a success message instead of an error message.
	}   
	
	/**
	 * Prova a cancellare un bucket, FALLISCE se non è vuoto 
	 * @param bucketName
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public void deleteBucket(String bucketName) throws AmazonServiceException, AmazonClientException{
		  s3.deleteBucket(bucketName);		  
	}
	
	/**
	 * Genera un nome univoco per il file associato ad un particolare messaggio di posta 
	 * @param mail
	 * @return
	 */
	public String generateFileKey(MetaMail mail){
		return mail.getFolder()+"_"+mail.getUid();
	}
	
	public String getCurrentBucket(){
		
		if(this.currentEndPoint == null)
			return TableInfo.BucketNameUSStandard.toString();		
		else if(this.currentEndPoint.toString().compareTo(AmazonEndPoint.S3EUIreland.toString()) == 0)
			return TableInfo.BucketNameEUWest.toString();
		//else if per ogni AmazonEndPoint.S3qualcosa (e bisogna anche creare TableInfo.BucketNameQualcosa per ognuno)
		else
			return TableInfo.BucketNameUSStandard.toString();
	}
	
	/****************************************************Funzioni di Print**************************************************/
	
	public void printS3Object(S3Object object)	{
	    System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
	    try {
			displayTextInputStream(object.getObjectContent());
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}
	
	public void printBucketList(){
		System.out.println("Listing buckets");
	    for (Bucket bucket : s3.listBuckets()) {
	        System.out.println(" - " + bucket.getName());
	    }
	}	
	
	/****************************************************Funzioni di prova**************************************************/

	public void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }

}