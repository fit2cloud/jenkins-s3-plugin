package com.fit2cloud.jenkins.s3;

public class AWSS3Exception extends Exception {

	private static final long serialVersionUID = 1582215285822395979L;

	public AWSS3Exception() {
		super();
	}

	public AWSS3Exception(final String message, final Throwable cause) {
		super(message,cause); 
	}

	public AWSS3Exception(final String message) {
		super(message);
	}
}
