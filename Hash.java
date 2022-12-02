package hw7;
import java.math.BigInteger;
import java.security.*;

/***************************************************/
/* CS-350 Fall 2022 - Homework 7 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a simple MD5 hash calculator. It takes in     */
/*   input an integer value and produces in output */
/*   a string with the hexadecimal representation  */
/*   of the corresponding MD5 hash.                */
/*                                                 */
/***************************************************/

public class Hash {

    public String hash(String numString) throws NoSuchAlgorithmException 
    {
    	/* Construct new MD5 digestor */
        MessageDigest md5dig = MessageDigest.getInstance("MD5");

	/* Go from input decimal number to MD5 numerical hash value */
        byte[] hashBin = md5dig.digest(numString.getBytes());
        BigInteger hashInt = new BigInteger(1, hashBin);

        /* Perform conversion to hexadecimal ASCII representation */
        String retval = hashInt.toString(16);
        
        /* Make sure that there is always enough initial zeros for the length to be 32 bytes */
        while (retval.length() < 32) {
            retval = "0" + retval;
        }
        
        /* Return the result */
        return retval;
    }

}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
