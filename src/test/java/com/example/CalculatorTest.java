package com.example;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.epam.tat.module4.Calculator;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;




public class CalculatorTest {
    static ExtentReports reports;
    static ExtentTest test;
    static ExtentReports extent = new ExtentReports();

    private Calculator calculator;

    @BeforeTest
    public void setUp() {
        calculator = new Calculator();
        System.out.println("Calculator instance initialized.");
        ExtentSparkReporter spark= new ExtentSparkReporter("target/extentreport.html");
        extent.attachReporter(spark);
    }

    @AfterTest
    public void tearDown() {
        System.out.println("Calculator instance destroyed.");
        // Clean up any resources if needed
    }

    // Positive Test Cases
    @Test
    public void testSumPositive() {
        test= extent.createTest("testSumPositive","The function validates testSumPositive");
        long result = calculator.sum(5, 3);
        Assert.assertEquals(8, result);
        extent.flush();
    }

    @Test
    public void testSubPositive() {
        double result = calculator.sub(5.5, 2.2);
        Assert.assertEquals(3.3, result, 0.0001);
    }

    @Test
    public void testMultPositive() {
        long result = calculator.mult(4, 3);
        Assert.assertEquals(12, result);
    }

    @Test
    public void testDivPositive() {
        double result = calculator.div(10, 2);
        Assert.assertEquals(5, result, 0.0001);
    }

    @Test
    public void testIsPositivePositive() {
        boolean result = calculator.isPositive(5);
        Assert.assertTrue(result);
    }

    // Negative Test Cases
    @Test(expectedExceptions=NumberFormatException.class)
    public void testDivByZero() {
        calculator.div(10, 0);
    }

    @Test
    public void testSubNegative() {
        double result = calculator.sub(2.2, 5.5);
        Assert.assertEquals(-3.3, result, 0.0001);
    }

    @Test
    public void testMultNegative() {
        long result = calculator.mult(-4, 3);
        Assert.assertEquals(-12, result);
    }

    @Test
    public void testIsPositiveNegative() {
        boolean result = calculator.isPositive(-5);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsNegative() {
        boolean result = calculator.isNegative(5);
        Assert.assertFalse(result);
    }
}
