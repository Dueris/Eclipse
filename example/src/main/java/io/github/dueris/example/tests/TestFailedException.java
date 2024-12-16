package io.github.dueris.example.tests;

public class TestFailedException extends RuntimeException {
	public TestFailedException(String test, Throwable throwable) {
		super("Test failed! " + test, throwable);
	}
}
