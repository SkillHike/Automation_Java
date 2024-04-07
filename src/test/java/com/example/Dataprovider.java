package com.example;

import com.epam.tat.module4.Calculator;
import org.testng.Assert;
import org.testng.annotations.*;

public class Dataprovider {

    private Calculator calculator;

    @BeforeTest
    public void setUp() {
        calculator = new Calculator();
        System.out.println("Calculator instance initialized.");
    }

    @AfterTest
    public void tearDown() {
        System.out.println("Calculator instance destroyed.");
        // Clean up any resources if needed
    }

    @DataProvider(name = "testData")
    public Object[][] testData() {
        return new Object[][]{
                {5, 3, 8},
                {5.5, 2.2, 3.3},
                {4, 3, 12},
                {10, 2, 5}
        };
    }

    @Test(dataProvider = "testData")
    public void testOperations(double num1, double num2, double expectedResult) {
        double result = calculator.sum(num1, num2);
        Assert.assertEquals(result, expectedResult);
    }
}
