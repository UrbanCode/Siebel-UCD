/**
 * © Copyright IBM Corporation 2016.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.plugin.tests;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


// Required to demonstrate the CommandHelper test example below.
/**
 *import java.io.File;
 *import com.urbancode.air.CommandHelper;
 */
public class ExampleTest {

   @BeforeClass
   public static void beforeClassTest() {
       System.out.println("Before Class Test Sample");
   }
    
   @AfterClass
   public static void afterClassTest() {
       System.out.println("After Class Test Sample");
   }
   
   @Before
   public void beforeTest() {
       System.out.println("Before Test Sample");
   }
   
   @After
   public void afterTest() {
       System.out.println("After Test Sample");
   }
    
   @Test
   public void testAssert() {
      String oneStr = "Hello World";
      String twoStr = "Hello World";
      assertEquals(oneStr, twoStr);
   }
  
    //If you were testing a Groovy class, such as CommandHelper, you may write a test like this...
   /**
    *@Test
    *public void testCommandHelper() {
    *    CommandHelper ch = new CommandHelper(new File("."));
    *    String[] array = new String[]{"echo", "Hello World!"};
    *    ch.runCommand("Printing 'Hello World'...", array);
    *}
    */
}