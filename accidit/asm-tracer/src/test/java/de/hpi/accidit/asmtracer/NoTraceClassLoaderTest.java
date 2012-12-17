package de.hpi.accidit.asmtracer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author derari
 */
public class NoTraceClassLoaderTest {
    
    public NoTraceClassLoaderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of addClass method, of class NoTraceClassLoader.
     */
    @Test
    public void testAddClass() throws Exception {
        PreMain.Init.createOut();
    }
}
