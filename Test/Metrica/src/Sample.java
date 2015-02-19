import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;



public class Sample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		// TODO Auto-generated method stub
		int[] regioni = new int[4];
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String value = null;
		
		for(int i=0; i<500; ++i){
			
			System.out.print("Enter option: ");
			try {
				value = br.readLine();
			} catch (IOException e) {
				System.out.println("Error!");
				System.exit(1);
			}
			
	        switch (value) {
	        	case "0":
	            	for(int j=0; j<regioni.length; ++j){
		            	if(j == 0)
		            		regioni[j]++;
		            	else 
		            		if(regioni[j] != 0)
		            			regioni[j]--;
		            }
		            break;
	            case "1":
	            	for(int j=0; j<regioni.length; ++j){
		            	if(j == 1)
		            		regioni[j]++;
		            	else 
		            		if(regioni[j] != 0)
		            			regioni[j]--;
		            }
		            break;
	            case "2":  
		            for(int j=0; j<regioni.length; ++j){
		            	if(j == 2)
		            		regioni[j]++;
		            	else 
		            		if(regioni[j] != 0)
		            			regioni[j]--;
		            }
		            break;
	            case "3":  
		            for(int j=0; j<regioni.length; ++j){
		            	if(j == 3)
		            		regioni[j]++;
		            	else 
		            		if(regioni[j] != 0)
		            			regioni[j]--;
		            }
		            break;
	            default: break;
	        }
	        
	        for(int k=0; k<regioni.length; ++k)
	        	System.out.println(regioni[k]);
	        System.out.println("");
			
		}

	}

}
